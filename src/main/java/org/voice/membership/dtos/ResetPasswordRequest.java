package org.voice.membership.dtos;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import org.voice.membership.validation.StrongPassword;

public record ResetPasswordRequest(
        @NotBlank(message = "Token is required")
        String token,
        @StrongPassword
        String password,
        @NotBlank(message = "Confirm password is required")
        String confirmPassword) {

    @AssertTrue(message = "Passwords do not match")
    public boolean isPasswordMatch() {
        return password != null && password.equals(confirmPassword);
    }
}
