package org.voice.membership.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.voice.membership.config.PayPalProperties;
import org.voice.membership.dtos.CapturePayPalOrderRequest;
import org.voice.membership.dtos.PayPalOrderResponse;
import org.voice.membership.dtos.RedirectResponse;
import org.voice.membership.dtos.WebhookStatusResponse;
import org.voice.membership.entities.Membership;
import org.voice.membership.entities.MembershipPaymentTransaction;
import org.voice.membership.entities.User;
import org.voice.membership.exceptions.BadRequestException;
import org.voice.membership.exceptions.UnauthorizedException;
import org.voice.membership.repositories.MembershipPaymentTransactionRepository;
import org.voice.membership.repositories.MembershipRepository;
import org.voice.membership.repositories.UserRepository;

import java.math.RoundingMode;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayPalMembershipService {

    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final MembershipPaymentTransactionRepository paymentTransactionRepository;
    private final PayPalService payPalService;
    private final PayPalProperties payPalProperties;
    private final ObjectMapper objectMapper;
    private final MembershipService membershipService;

    public PayPalOrderResponse createOrder(Integer membershipId, String userEmail) {
        if (!payPalProperties.hasCredentials()) {
            throw new IllegalStateException("PayPal is not configured");
        }
        if (membershipId == null) {
            throw new BadRequestException("Membership is required");
        }

        User user = userRepository.findByEmail(userEmail);
        if (user == null) {
            throw new UnauthorizedException("Unauthorized");
        }

        Membership currentMembership = user.getMembership();
        if (currentMembership == null || !currentMembership.isFree()) {
            throw new BadRequestException("Not eligible for membership upgrade");
        }

        Membership paidMembership = membershipRepository.findById(membershipId)
                .filter(m -> !m.isFree())
                .orElseThrow(() -> new BadRequestException("Invalid membership"));

        String orderId;
        try {
            orderId = payPalService.createOrder(user, paidMembership);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create payment order", e);
        }

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

        return PayPalOrderResponse.builder().orderId(orderId).build();
    }

    public RedirectResponse captureOrder(CapturePayPalOrderRequest request, String userEmail) {
        Integer membershipId = java.util.Objects.requireNonNull(request.membershipId(), "Membership is required");
        User user = userRepository.findByEmail(userEmail);
        if (user == null) {
            throw new UnauthorizedException("Unauthorized");
        }

        Membership currentMembership = user.getMembership();
        if (currentMembership == null || !currentMembership.isFree()) {
            throw new BadRequestException("Not eligible for membership upgrade");
        }

        Membership paidMembership = membershipRepository.findById(membershipId)
                .filter(m -> !m.isFree())
                .orElseThrow(() -> new BadRequestException("Invalid membership"));

        MembershipPaymentTransaction transaction = paymentTransactionRepository
                .findByPaypalOrderId(request.orderId())
                .orElseGet(MembershipPaymentTransaction::new);

        if (transaction.getId() != null && transaction.getUser() != null
            && transaction.getUser().getId() != user.getId()) {
            throw new UnauthorizedException("Order does not belong to user");
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
            return RedirectResponse.builder().success(true).redirectUrl("/profile?upgrade=success").build();
        }

        PayPalService.CaptureValidationResult validation;
        try {
            validation = payPalService.captureAndValidateOrder(
                    user,
                    paidMembership,
                    request.orderId());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to finalize payment", e);
        }

        if (!validation.completed()) {
            transaction.setStatus("FAILED");
            transaction.setFailureReason(validation.message());
            paymentTransactionRepository.save(transaction);
            throw new BadRequestException("Payment verification failed");
        }

        membershipService.applyMembershipUpgrade(user, paidMembership);
        transaction.setPaypalCaptureId(validation.captureId());
        transaction.setStatus("COMPLETED");
        transaction.setFailureReason(null);
        paymentTransactionRepository.save(transaction);

        log.info("Membership upgraded after PayPal capture. userId={} membershipId={} orderId={} captureId={}",
                user.getId(), paidMembership.getId(), request.orderId(), validation.captureId());

        return RedirectResponse.builder().success(true).redirectUrl("/profile?upgrade=success").build();
    }

    public WebhookStatusResponse handleWebhook(Map<String, String> headers, String payload) {
        try {
            JsonNode event = objectMapper.readTree(payload);
            boolean verified = payPalService.verifyWebhookSignature(normalizeHeaders(headers), event);

            if (!verified) {
                throw new BadRequestException("invalid_signature");
            }

            String eventType = event.path("event_type").asText();
            if (!"PAYMENT.CAPTURE.COMPLETED".equalsIgnoreCase(eventType)) {
                return WebhookStatusResponse.builder().status("ignored").build();
            }

            JsonNode resource = event.path("resource");
            String orderId = resource.path("supplementary_data").path("related_ids").path("order_id").asText();
            String captureId = resource.path("id").asText();

            if (orderId == null || orderId.isBlank()) {
                return WebhookStatusResponse.builder().status("ignored_missing_order").build();
            }

            Optional<MembershipPaymentTransaction> transactionOpt = paymentTransactionRepository
                    .findByPaypalOrderId(orderId);
            if (transactionOpt.isEmpty()) {
                return WebhookStatusResponse.builder().status("ignored_unknown_order").build();
            }

            MembershipPaymentTransaction transaction = transactionOpt.get();
            if ("COMPLETED".equalsIgnoreCase(transaction.getStatus())) {
                return WebhookStatusResponse.builder().status("already_completed").build();
            }

            User user = transaction.getUser();
            Membership membership = transaction.getMembership();

            PayPalService.CaptureValidationResult validation = payPalService.validateOrderFromPayPal(user, membership,
                    orderId);
            if (!validation.completed()) {
                transaction.setStatus("FAILED");
                transaction.setFailureReason("Webhook validation failed: " + validation.message());
                paymentTransactionRepository.save(transaction);
                return WebhookStatusResponse.builder().status("validation_failed").build();
            }

            membershipService.applyMembershipUpgrade(user, membership);
            transaction.setStatus("COMPLETED");
            transaction.setPaypalCaptureId(captureId);
            transaction.setFailureReason(null);
            paymentTransactionRepository.save(transaction);

            log.info("Membership upgraded from PayPal webhook. userId={} membershipId={} orderId={} captureId={}",
                    user.getId(), membership.getId(), orderId, captureId);

            return WebhookStatusResponse.builder().status("processed").build();
        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("PayPal webhook processing failed", ex);
            throw new IllegalStateException("Webhook processing failed", ex);
        }
    }

    private Map<String, String> normalizeHeaders(Map<String, String> headers) {
        return headers.entrySet().stream()
                .filter(entry -> entry.getKey() != null && entry.getValue() != null)
                .collect(Collectors.toMap(
                        entry -> entry.getKey().toLowerCase(Locale.ROOT),
                        Map.Entry::getValue,
                        (oldVal, newVal) -> oldVal));
    }
}
