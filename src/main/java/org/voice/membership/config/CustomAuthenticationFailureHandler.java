package org.voice.membership.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.voice.membership.services.AccountLockoutService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.util.ContentCachingRequestWrapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Custom authentication failure handler that redirects to appropriate error
 * pages
 * based on the type of authentication failure.
 */
@Component
public class CustomAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Autowired
    private AccountLockoutService accountLockoutService;

    /**
     * Extracts username from request by trying multiple methods.
     * First tries request parameters, then tries to parse from cached request body.
     */
    private String extractUsername(HttpServletRequest request) {
        // First, try to get from request parameters (works if not consumed yet)
        String username = request.getParameter("username");
        if (username != null && !username.isEmpty()) {
            return username;
        }

        // Second, try to get from cached request wrapper
        if (request instanceof ContentCachingRequestWrapper) {
            ContentCachingRequestWrapper cachedRequest = (ContentCachingRequestWrapper) request;
            byte[] cachedBody = cachedRequest.getContentAsByteArray();
            if (cachedBody.length > 0) {
                try {
                    String body = new String(cachedBody, StandardCharsets.UTF_8);
                    return parseUsernameFromBody(body);
                } catch (Exception e) {
                    // If parsing fails, continue to next method
                }
            }
        }

        // Third, try to parse from request input stream if available
        try {
            byte[] body = request.getInputStream().readAllBytes();
            if (body.length > 0) {
                String bodyStr = new String(body, StandardCharsets.UTF_8);
                return parseUsernameFromBody(bodyStr);
            }
        } catch (Exception e) {
            // If reading fails, username remains null
        }

        return username;
    }

    /**
     * Parses URL-encoded form data to extract the username parameter.
     * Example: "username=user@example.com&password=pass&remember-me=on"
     */
    private String parseUsernameFromBody(String body) {
        if (body == null || body.isEmpty()) {
            return null;
        }

        String[] params = body.split("&");
        for (String param : params) {
            if (param.startsWith("username=")) {
                String username = param.substring("username=".length());
                // URL decode the username
                try {
                    return java.net.URLDecoder.decode(username, StandardCharsets.UTF_8.name());
                } catch (Exception e) {
                    return username;
                }
            }
        }
        return null;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) throws IOException, ServletException {

        String errorMessage = exception.getMessage();
        String username = extractUsername(request); // Use helper method instead of direct getParameter

        // Check if the failure is due to account lockout
        if (exception instanceof LockedException) {
            long remainingMinutes = accountLockoutService.getRemainingLockoutTime(username);
            setDefaultFailureUrl("/login?locked=true&minutes=" + remainingMinutes);
        }
        // Check if the failure is due to unverified email
        else if (errorMessage != null && errorMessage.toLowerCase().contains("verify your email")) {
            setDefaultFailureUrl("/login?unverified=true");
        }
        // Regular authentication failure (bad credentials)
        else {
            // Record failed login attempt
            if (username != null && !username.isEmpty()) {
                accountLockoutService.recordFailedLoginAttempt(username);

                // Check if account is now locked after this attempt
                if (accountLockoutService.isAccountLocked(username)) {
                    long remainingMinutes = accountLockoutService.getRemainingLockoutTime(username);
                    setDefaultFailureUrl("/login?locked=true&minutes=" + remainingMinutes);
                } else {
                    // Show remaining attempts
                    int remainingAttempts = accountLockoutService.getRemainingAttempts(username);
                    setDefaultFailureUrl("/login?error=true&remaining=" + remainingAttempts);
                }
            } else {
                setDefaultFailureUrl("/login?error=true");
            }
        }

        super.onAuthenticationFailure(request, response, exception);
    }
}
