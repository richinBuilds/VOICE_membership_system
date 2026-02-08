package org.voice.membership.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validates that a password meets the configured strong password rules.
 * Used by the {@link StrongPassword} annotation during form validation.
 * Delegates validation logic to {@link PasswordPolicy} to avoid code duplication.
 */
public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return PasswordPolicy.isStrong(value);
    }
}

