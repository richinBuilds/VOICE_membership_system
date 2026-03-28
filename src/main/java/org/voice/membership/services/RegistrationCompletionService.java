package org.voice.membership.services;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.voice.membership.dtos.MultiStepRegistrationDto;

import java.math.BigDecimal;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class RegistrationCompletionService {

    private final RegistrationService registrationService;

    public String completeRegistration(HttpSession session) {
        try {
            MultiStepRegistrationDto registrationData = (MultiStepRegistrationDto) session
                    .getAttribute("registrationData");
            if (registrationData == null || registrationData.getUserDetails() == null) {
                return "redirect:/register";
            }

            Integer googleSignupUserId = (Integer) session
                    .getAttribute(GoogleOAuth2UserService.GOOGLE_SIGNUP_USER_ID_SESSION_KEY);
            String paypalOrderId = (String) session.getAttribute("paypalOrderId");
            String paypalCaptureId = (String) session.getAttribute("paypalCaptureId");
            BigDecimal paymentAmount = (BigDecimal) session.getAttribute("paymentAmount");
            boolean paymentCompleted = Boolean.TRUE.equals(session.getAttribute("registrationPaymentCompleted"));

            registrationService.registerUser(
                    registrationData.getUserDetails(),
                    googleSignupUserId,
                    registrationData.getSelectedMembershipId(),
                    registrationData.getChildren(),
                    paypalOrderId,
                    paypalCaptureId,
                    paymentAmount);

            // Clean up session
            session.removeAttribute("registrationData");
            session.removeAttribute("registrationPaymentRef");
            session.removeAttribute("registrationPayPalOrderId");
            session.removeAttribute("registrationPaymentCompleted");
            session.removeAttribute("paypalOrderId");
            session.removeAttribute("paypalCaptureId");
            session.removeAttribute("paymentAmount");
            session.removeAttribute(GoogleOAuth2UserService.GOOGLE_SIGNUP_USER_ID_SESSION_KEY);
            session.removeAttribute(GoogleOAuth2UserService.GOOGLE_SIGNUP_REDIRECT_STEP2_SESSION_KEY);

            return paymentCompleted
                    ? "redirect:/register/verification-sent?payment=success"
                    : "redirect:/register/verification-sent";

        } catch (NoSuchElementException e) {
            session.removeAttribute(GoogleOAuth2UserService.GOOGLE_SIGNUP_USER_ID_SESSION_KEY);
            return "redirect:/register?error=registration_session_invalid";
        } catch (Exception e) {
            return "redirect:/register?error=registration_failed";
        }
    }
}
