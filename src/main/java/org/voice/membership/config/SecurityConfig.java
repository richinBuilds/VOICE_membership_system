package org.voice.membership.config;

import org.voice.membership.entities.Role;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.filter.OncePerRequestFilter;
import org.voice.membership.services.GoogleOAuth2UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Configuration
@EnableWebSecurity
/**
 * Configures Spring Security for the VOICE membership application. Defines
 * public and protected routes, login/logout, remember-me, and redirects.
 */
public class SecurityConfig {

        @Autowired
        private CustomAuthenticationFailureHandler authenticationFailureHandler;

        @Autowired
        private CustomAuthenticationSuccessHandler authenticationSuccessHandler;

        /**
         * Filter to cache request content so it can be read multiple times.
         * This allows the failure handler to extract the username from the request
         * body.
         */
        @Bean
        public OncePerRequestFilter contentCachingFilter() {
                return new OncePerRequestFilter() {
                        @Override
                        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                        FilterChain filterChain) throws ServletException, IOException {
                                // Only wrap POST requests to /login to cache their content
                                if ("POST".equals(request.getMethod()) && request.getRequestURI().equals("/login")) {
                                        ContentCachingRequestWrapper cachingRequest = new ContentCachingRequestWrapper(
                                                        request);
                                        filterChain.doFilter(cachingRequest, response);
                                } else {
                                        filterChain.doFilter(request, response);
                                }
                        }
                };
        }

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity,
                        GoogleOAuth2UserService googleOAuth2UserService) throws Exception {
                return httpSecurity
                                .addFilterBefore(contentCachingFilter(),
                                                org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)
                                .authorizeHttpRequests(auth -> auth

                                                .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()
                                                .requestMatchers("/").permitAll()
                                                .requestMatchers("/login").permitAll()
                                                .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                                                .requestMatchers("/register/paypal/checkout/**").permitAll()
                                                .requestMatchers("/register/paypal/**")
                                                .hasAnyRole(Role.USER.name(), Role.ADMIN.name())
                                                .requestMatchers("/register/**").permitAll()
                                                .requestMatchers("/forgot-password").permitAll()
                                                .requestMatchers("/reset-password").permitAll()
                                                .requestMatchers("/api/landing-page/**").permitAll()
                                                .requestMatchers("/api/paypal/webhook").permitAll()

                                                .requestMatchers("/admin/**").hasRole(Role.ADMIN.name())
                                                .requestMatchers("/api/admin/**").hasRole(Role.ADMIN.name())

                                                .requestMatchers("/profile/**")
                                                .hasAnyRole(Role.USER.name(), Role.ADMIN.name())

                                                .anyRequest().authenticated())

                                .formLogin(form -> form
                                                .loginPage("/login")
                                                .successHandler(authenticationSuccessHandler)
                                                .failureHandler(authenticationFailureHandler)
                                                .permitAll())
                                .oauth2Login(oauth2 -> oauth2
                                                .loginPage("/login")
                                                .userInfoEndpoint(userInfo -> userInfo
                                                                .userService(googleOAuth2UserService))
                                                .failureHandler((request, response, exception) -> {
                                                        request.getSession().removeAttribute(
                                                                        GoogleOAuth2UserService.GOOGLE_AUTH_FLOW_SESSION_KEY);
                                                        request.getSession().removeAttribute(
                                                                        GoogleOAuth2UserService.GOOGLE_SIGNUP_REDIRECT_STEP2_SESSION_KEY);
                                                        request.getSession().removeAttribute(
                                                                        GoogleOAuth2UserService.GOOGLE_SIGNUP_USER_ID_SESSION_KEY);

                                                        String redirectUrl = "/login?error=true";
                                                        if (exception instanceof OAuth2AuthenticationException oauth2Exception
                                                                        && oauth2Exception.getError() != null
                                                                        && "email_exists".equalsIgnoreCase(
                                                                                        oauth2Exception.getError()
                                                                                                        .getErrorCode())) {

                                                                redirectUrl = "/register?error=email_exists";
                                                        } else if (exception instanceof OAuth2AuthenticationException oauth2Exception
                                                                        && oauth2Exception.getError() != null
                                                                        && "google_signup_required".equalsIgnoreCase(
                                                                                        oauth2Exception.getError()
                                                                                                        .getErrorCode())) {
                                                                redirectUrl = "/login?googleSignupRequired=true";
                                                        } else if (exception instanceof OAuth2AuthenticationException oauth2Exception
                                                                        && oauth2Exception.getError() != null
                                                                        && "email_unverified".equalsIgnoreCase(
                                                                                        oauth2Exception.getError()
                                                                                                        .getErrorCode())) {
                                                                redirectUrl = "/login?unverified=true";
                                                        } else if (exception instanceof OAuth2AuthenticationException oauth2Exception
                                                                        && oauth2Exception.getError() != null
                                                                        && "account_locked".equalsIgnoreCase(
                                                                                        oauth2Exception.getError()
                                                                                                        .getErrorCode())) {
                                                                redirectUrl = "/login?locked=true";
                                                        } else if (exception instanceof OAuth2AuthenticationException oauth2Exception
                                                                        && oauth2Exception.getError() != null
                                                                        && "invalid_google_account".equalsIgnoreCase(
                                                                                        oauth2Exception.getError()
                                                                                                        .getErrorCode())) {
                                                                redirectUrl = "/login?invalidGoogleAccount=true";
                                                        }

                                                        response.sendRedirect(redirectUrl);
                                                })
                                                .successHandler(authenticationSuccessHandler))
                                .logout(config -> config
                                                .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                                                .logoutSuccessUrl("/")
                                                .invalidateHttpSession(true)
                                                .deleteCookies("VOICE_REMEMBER_ME", "JSESSIONID"))
                                .rememberMe(remember -> remember
                                                .key("voiceRememberMeKey")
                                                .tokenValiditySeconds(604800) // 7 days
                                                .rememberMeParameter("remember-me")
                                                .rememberMeCookieName("VOICE_REMEMBER_ME")
                                                .useSecureCookie(false) // Set to true in production with HTTPS
                                                .alwaysRemember(false))
                                .csrf(csrf -> csrf.ignoringRequestMatchers("/logout", "/api/paypal/webhook",
                                                "/api/admin/notifications/**"))
                                .build();
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

}
