package org.voice.membership.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.voice.membership.config.PayPalProperties;
import org.voice.membership.dtos.CapturePayPalOrderRequest;
import org.voice.membership.dtos.CreatePayPalOrderRequest;
import org.voice.membership.entities.Membership;
import org.voice.membership.entities.MembershipPaymentTransaction;
import org.voice.membership.entities.User;
import org.voice.membership.repositories.MembershipPaymentTransactionRepository;
import org.voice.membership.repositories.MembershipRepository;
import org.voice.membership.repositories.UserRepository;
import org.voice.membership.services.PayPalService;

import java.math.RoundingMode;
import java.security.Principal;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequiredArgsConstructor
public class PayPalController {

    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final MembershipPaymentTransactionRepository paymentTransactionRepository;
    private final PayPalService payPalService;
    private final PayPalProperties payPalProperties;
    private final ObjectMapper objectMapper;

    @PostMapping("/register/paypal/create-order")
    public ResponseEntity<Map<String, Object>> createOrder(@RequestBody CreatePayPalOrderRequest request,
            Principal principal) {
        try {
            if (!payPalProperties.hasCredentials()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("message", "PayPal is not configured"));
            }

            if (request == null || request.membershipId() == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Membership is required"));
            }

            User user = userRepository.findByEmail(principal.getName());
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Unauthorized"));
            }

            Membership currentMembership = user.getMembership();
            if (currentMembership == null || !currentMembership.isFree()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Not eligible for membership upgrade"));
            }

            Optional<Membership> membershipOpt = membershipRepository.findById(request.membershipId());
            if (membershipOpt.isEmpty() || membershipOpt.get().isFree()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Invalid membership"));
            }

            Membership paidMembership = membershipOpt.get();

            String orderId = payPalService.createOrder(user, paidMembership);

            MembershipPaymentTransaction transaction = paymentTransactionRepository.findByPaypalOrderId(orderId)
                    .orElseGet(MembershipPaymentTransaction::new);
            transaction.setUser(user);
            transaction.setMembership(paidMembership);
            transaction.setPaypalOrderId(orderId);
            transaction.setAmount(paidMembership.getPrice().setScale(2, RoundingMode.HALF_UP));
            transaction.setCurrency(payPalProperties.getCurrency());
            transaction.setStatus("CREATED");
            transaction.setFailureReason(null);
            paymentTransactionRepository.save(transaction);

            return ResponseEntity.ok(Map.of("orderId", orderId));
        } catch (Exception ex) {
            log.error("Failed to create PayPal order", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to create payment order"));
        }
    }

    @PostMapping("/register/paypal/capture-order")
    public ResponseEntity<Map<String, Object>> captureOrder(@RequestBody CapturePayPalOrderRequest request,
            Principal principal) {
        try {
            if (request == null || request.membershipId() == null || request.orderId() == null
                    || request.orderId().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Membership and order are required"));
            }

            User user = userRepository.findByEmail(principal.getName());
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Unauthorized"));
            }

            Membership currentMembership = user.getMembership();
            if (currentMembership == null || !currentMembership.isFree()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Not eligible for membership upgrade"));
            }

            Optional<Membership> membershipOpt = membershipRepository.findById(request.membershipId());
            if (membershipOpt.isEmpty() || membershipOpt.get().isFree()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Invalid membership"));
            }
            Membership paidMembership = membershipOpt.get();

            MembershipPaymentTransaction transaction = paymentTransactionRepository
                    .findByPaypalOrderId(request.orderId())
                    .orElseGet(MembershipPaymentTransaction::new);

            if (transaction.getId() != null && transaction.getUser() != null
                    && transaction.getUser().getId() != user.getId()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Order does not belong to user"));
            }

            transaction.setUser(user);
            transaction.setMembership(paidMembership);
            transaction.setPaypalOrderId(request.orderId());
            transaction.setAmount(paidMembership.getPrice().setScale(2, RoundingMode.HALF_UP));
            transaction.setCurrency(payPalProperties.getCurrency());
            if (!"COMPLETED".equalsIgnoreCase(transaction.getStatus())) {
                transaction.setStatus("CAPTURE_PENDING");
            }
            paymentTransactionRepository.save(transaction);

            if ("COMPLETED".equalsIgnoreCase(transaction.getStatus())) {
                return ResponseEntity.ok(Map.of("success", true, "redirectUrl", "/profile?upgrade=success"));
            }

            PayPalService.CaptureValidationResult validation = payPalService.captureAndValidateOrder(
                    user,
                    paidMembership,
                    request.orderId());

            if (!validation.completed()) {
                transaction.setStatus("FAILED");
                transaction.setFailureReason(validation.message());
                paymentTransactionRepository.save(transaction);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Payment verification failed"));
            }

            applyMembershipUpgrade(user, paidMembership);
            transaction.setPaypalCaptureId(validation.captureId());
            transaction.setStatus("COMPLETED");
            transaction.setFailureReason(null);
            paymentTransactionRepository.save(transaction);

            log.info("Membership upgraded after PayPal capture. userId={} membershipId={} orderId={} captureId={}",
                    user.getId(), paidMembership.getId(), request.orderId(), validation.captureId());

            return ResponseEntity.ok(Map.of("success", true, "redirectUrl", "/profile?upgrade=success"));
        } catch (Exception ex) {
            log.error("Failed to capture PayPal order", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to finalize payment"));
        }
    }

    @PostMapping("/api/paypal/webhook")
    public ResponseEntity<Map<String, String>> webhook(@RequestHeader Map<String, String> headers,
            @RequestBody String payload) {
        try {
            JsonNode event = objectMapper.readTree(payload);
            boolean verified = payPalService.verifyWebhookSignature(normalizeHeaders(headers), event);

            if (!verified) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("status", "invalid_signature"));
            }

            String eventType = event.path("event_type").asText();
            if (!"PAYMENT.CAPTURE.COMPLETED".equalsIgnoreCase(eventType)) {
                return ResponseEntity.ok(Map.of("status", "ignored"));
            }

            JsonNode resource = event.path("resource");
            String orderId = resource.path("supplementary_data").path("related_ids").path("order_id").asText();
            String captureId = resource.path("id").asText();

            if (orderId == null || orderId.isBlank()) {
                return ResponseEntity.ok(Map.of("status", "ignored_missing_order"));
            }

            Optional<MembershipPaymentTransaction> transactionOpt = paymentTransactionRepository
                    .findByPaypalOrderId(orderId);
            if (transactionOpt.isEmpty()) {
                return ResponseEntity.ok(Map.of("status", "ignored_unknown_order"));
            }

            MembershipPaymentTransaction transaction = transactionOpt.get();
            if ("COMPLETED".equalsIgnoreCase(transaction.getStatus())) {
                return ResponseEntity.ok(Map.of("status", "already_completed"));
            }

            User user = transaction.getUser();
            Membership membership = transaction.getMembership();

            PayPalService.CaptureValidationResult validation = payPalService.validateOrderFromPayPal(user, membership,
                    orderId);
            if (!validation.completed()) {
                transaction.setStatus("FAILED");
                transaction.setFailureReason("Webhook validation failed: " + validation.message());
                paymentTransactionRepository.save(transaction);
                return ResponseEntity.ok(Map.of("status", "validation_failed"));
            }

            applyMembershipUpgrade(user, membership);
            transaction.setStatus("COMPLETED");
            transaction.setPaypalCaptureId(captureId);
            transaction.setFailureReason(null);
            paymentTransactionRepository.save(transaction);

            log.info("Membership upgraded from PayPal webhook. userId={} membershipId={} orderId={} captureId={}",
                    user.getId(), membership.getId(), orderId, captureId);

            return ResponseEntity.ok(Map.of("status", "processed"));
        } catch (Exception ex) {
            log.error("PayPal webhook processing failed", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", "error"));
        }
    }

    private void applyMembershipUpgrade(User user, Membership paidMembership) {
        user.setMembership(paidMembership);
        user.setPaid(true);

        Date now = new Date();
        user.setMembershipStartDate(now);

        Calendar cal = Calendar.getInstance();
        cal.setTime(now);
        cal.add(Calendar.YEAR, 1);
        user.setMembershipExpiryDate(cal.getTime());

        userRepository.save(user);
    }

    private Map<String, String> normalizeHeaders(Map<String, String> headers) {
        return headers.entrySet().stream()
                .filter(entry -> entry.getKey() != null && entry.getValue() != null)
                .collect(java.util.stream.Collectors.toMap(
                        entry -> entry.getKey().toLowerCase(Locale.ROOT),
                        Map.Entry::getValue,
                        (oldVal, newVal) -> oldVal));
    }
}
