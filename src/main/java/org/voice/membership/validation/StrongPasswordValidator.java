package org.voice.membership.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

/**
 * Validates that a password meets the configured strong password rules.
 * Used by the {@link StrongPassword} annotation during form validation.
 */
public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    private static final Pattern STRONG_PASSWORD = Pattern.compile(
            "^(?=.{8,64}$)(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[~`!@#$%^&*()_+\\-={}\\[\\]|:;\"'<>,.?/]).*(?!.*\\s).*$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return false;
        }
        return STRONG_PASSWORD.matcher(value).matches();
    }
}

