package org.voice.membership.services;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.voice.membership.config.PayPalProperties;
import org.voice.membership.dtos.CapturePayPalOrderRequest;
import org.voice.membership.dtos.MultiStepRegistrationDto;
import org.voice.membership.dtos.PayPalOrderResponse;
import org.voice.membership.dtos.RedirectResponse;
import org.voice.membership.entities.Membership;
import org.voice.membership.exceptions.BadRequestException;
import org.voice.membership.exceptions.UnauthorizedException;

import java.math.RoundingMode;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RegistrationCheckoutService {

    private final PayPalProperties payPalProperties;
    private final MembershipService membershipService;
    private final PayPalService payPalService;

    public PayPalOrderResponse createOrder(Integer requestMembershipId, HttpSession session) {
        if (!payPalProperties.hasCredentials()) {
            throw new IllegalStateException("PayPal is not configured");
        }

        MultiStepRegistrationDto registrationData = (MultiStepRegistrationDto) session.getAttribute("registrationData");
        if (registrationData == null) {
            throw new UnauthorizedException("Registration session expired");
        }

        Integer membershipId = registrationData.getCartMembershipId();
        if (membershipId == null) {
            throw new BadRequestException("No membership selected");
        }

        if (requestMembershipId == null || !requestMembershipId.equals(membershipId)) {
            throw new BadRequestException("Membership mismatch");
        }

        Membership membership = membershipService.getMembershipById(membershipId)
                .filter(m -> !m.isFree())
                .orElseThrow(() -> new BadRequestException("Invalid paid membership"));

        if (Boolean.TRUE.equals(session.getAttribute("registrationPaymentCompleted"))) {
            throw new BadRequestException("Payment already completed");
        }

        String registrationPaymentRef = (String) session.getAttribute("registrationPaymentRef");
        if (registrationPaymentRef == null || registrationPaymentRef.isBlank()) {
            registrationPaymentRef = UUID.randomUUID().toString();
            session.setAttribute("registrationPaymentRef", registrationPaymentRef);
        }

        String orderId;
        try {
            orderId = payPalService.createOrderForRegistration(membership, registrationPaymentRef);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create payment order", e);
        }
        session.setAttribute("registrationPayPalOrderId", orderId);

        return PayPalOrderResponse.builder().orderId(orderId).build();
    }

    public RedirectResponse captureOrder(CapturePayPalOrderRequest request, HttpSession session,
            RegistrationCompletionService completionService) {
        MultiStepRegistrationDto registrationData = (MultiStepRegistrationDto) session.getAttribute("registrationData");
        if (registrationData == null) {
            throw new UnauthorizedException("Registration session expired");
        }

        Integer membershipId = registrationData.getCartMembershipId();
        if (membershipId == null || request.orderId() == null || request.orderId().isBlank()) {
            throw new BadRequestException("Invalid payment request");
        }

        if (request.membershipId() == null || !request.membershipId().equals(membershipId)) {
            throw new BadRequestException("Membership mismatch");
        }

        String expectedOrderId = (String) session.getAttribute("registrationPayPalOrderId");
        if (expectedOrderId == null || !expectedOrderId.equals(request.orderId())) {
            throw new BadRequestException("Order mismatch");
        }

        Membership membership = membershipService.getMembershipById(membershipId)
                .filter(m -> !m.isFree())
                .orElseThrow(() -> new BadRequestException("Invalid paid membership"));

        String registrationPaymentRef = (String) session.getAttribute("registrationPaymentRef");
        if (registrationPaymentRef == null || registrationPaymentRef.isBlank()) {
            throw new BadRequestException("Payment reference missing");
        }

        PayPalService.CaptureValidationResult validation;
        try {
            validation = payPalService.captureAndValidateRegistration(
                    membership,
                    request.orderId(),
                    registrationPaymentRef);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to finalize payment", e);
        }

        if (!validation.completed()) {
            throw new BadRequestException("Payment verification failed");
        }

        session.setAttribute("registrationPaymentCompleted", true);
        session.setAttribute("paypalOrderId", request.orderId());
        session.setAttribute("paypalCaptureId", validation.captureId());
        session.setAttribute("paymentAmount", membership.getPrice().setScale(2, RoundingMode.HALF_UP));

        String redirect = completionService.completeRegistration(session);
        String redirectUrl = redirect.startsWith("redirect:") ? redirect.substring("redirect:".length()) : redirect;

        return RedirectResponse.builder()
                .success(true)
                .redirectUrl(redirectUrl)
                .build();
    }

    public Optional<Membership> resolveCheckoutMembership(HttpSession session) {
        MultiStepRegistrationDto registrationData = (MultiStepRegistrationDto) session.getAttribute("registrationData");
        if (registrationData == null || registrationData.getCartMembershipId() == null) {
            return Optional.empty();
        }
        return membershipService.getMembershipById(registrationData.getCartMembershipId());
    }
}
