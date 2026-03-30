package org.voice.membership.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.voice.membership.services.AccountLockoutService;
import org.voice.membership.services.GoogleOAuth2UserService;
import java.io.IOException;

/**
 * Custom authentication success handler that resets failed login attempts
 * and redirects users based on their role.
 */
@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Autowired
    private AccountLockoutService accountLockoutService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException {

        request.getSession().removeAttribute(GoogleOAuth2UserService.GOOGLE_AUTH_FLOW_SESSION_KEY);

        Object googleSignupRedirect = request.getSession().getAttribute(
                GoogleOAuth2UserService.GOOGLE_SIGNUP_REDIRECT_STEP2_SESSION_KEY);
        if (Boolean.TRUE.equals(googleSignupRedirect)) {
            request.getSession().removeAttribute(GoogleOAuth2UserService.GOOGLE_SIGNUP_REDIRECT_STEP2_SESSION_KEY);
            accountLockoutService.resetFailedAttempts(authentication.getName());

            SecurityContextHolder.clearContext();
            request.getSession().removeAttribute("SPRING_SECURITY_CONTEXT");

            response.sendRedirect("/register/step2");
            return;
        }

        // Reset failed login attempts on successful login
        String username = authentication.getName();
        accountLockoutService.resetFailedAttempts(username);

        // Redirect based on role
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN"));

        if (isAdmin) {
            response.sendRedirect("/admin/dashboard");
        } else {
            response.sendRedirect("/profile");
        }
    }
}
