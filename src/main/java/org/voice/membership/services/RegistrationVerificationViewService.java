package org.voice.membership.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.voice.membership.dtos.MessageViewData;

@Service
@RequiredArgsConstructor
public class RegistrationVerificationViewService {

    private final RegistrationService registrationService;

    public MessageViewData verify(String token) {
        return switch (registrationService.verifyEmail(token)) {
            case INVALID_TOKEN -> new MessageViewData(null, "Invalid verification token.");
            case EXPIRED -> new MessageViewData(null, "Verification token has expired. Please register again.");
            case SUCCESS -> new MessageViewData("Email verified successfully! You can now login to your account.", null);
        };
    }

    public MessageViewData resend(String email) {
        return switch (registrationService.resendVerification(email)) {
            case NOT_FOUND -> new MessageViewData(null, "No account found with this email address.");
            case ALREADY_VERIFIED -> new MessageViewData(null, "This email is already verified. You can login.");
            case EMAIL_SEND_FAILED -> new MessageViewData(null, "Failed to send verification email. Please try again later.");
            case SUCCESS -> new MessageViewData("Verification email sent! Please check your inbox.", null);
        };
    }
}
