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
import java.io.IOException;

/**
 * Custom authentication failure handler that redirects to appropriate error
 * pages
 * based on the type of authentication failure.
 */
@Component
public class CustomAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Autowired
    private AccountLockoutService accountLockoutService;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) throws IOException, ServletException {

        String errorMessage = exception.getMessage();
        String username = request.getParameter("username"); // email field

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
