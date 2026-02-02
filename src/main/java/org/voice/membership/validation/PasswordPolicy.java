package org.voice.membership.validation;

import java.util.regex.Pattern;

/**
 * Utility class that checks whether a password string is considered strong.
 * Used in places where direct password strength validation is required.
 */
public final class PasswordPolicy {
    private PasswordPolicy() {
    }

    private static final Pattern STRONG_PASSWORD = Pattern.compile(
            "^(?=.{8,64}$)(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[~`!@#$%^&*()_+\\-={}\\[\\]|:;\"'<>,.?/])(?!.*\\s).*$");

    public static boolean isStrong(String password) {
        return password != null && STRONG_PASSWORD.matcher(password).matches();
    }
}

