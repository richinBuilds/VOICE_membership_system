package org.voice.membership.validation;

import java.util.regex.Pattern;

public final class PasswordPolicy {
    private PasswordPolicy() {
    }

    private static final Pattern STRONG_PASSWORD = Pattern.compile(
            "^(?=.{8,64}$)(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[~`!@#$%^&*()_+\\-={}\\[\\]|:;\"'<>,.?/])(?!.*\\s).*$");

    public static boolean isStrong(String password) {
        return password != null && STRONG_PASSWORD.matcher(password).matches();
    }
}
