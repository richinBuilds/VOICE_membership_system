package org.voice.membership.config;

import org.voice.membership.entities.Role;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.core.Authentication;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
                return httpSecurity
                                .authorizeHttpRequests(auth -> auth

                                                // ✅ Allow static resources (IMPORTANT)
                                                .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()

                                                // ✅ Public pages
                                                .requestMatchers("/").permitAll()
                                                .requestMatchers("/login").permitAll()
                                                .requestMatchers("/register/**").permitAll()
                                                .requestMatchers("/forgot-password").permitAll()
                                                .requestMatchers("/reset-password").permitAll()
                                                .requestMatchers("/api/landing-page/**").permitAll()

                                                // ✅ Admin pages
                                                .requestMatchers("/admin/**").hasRole(Role.ADMIN.name())

                                                // ✅ User pages - allow both USER and ADMIN roles
                                                .requestMatchers("/profile/**")
                                                .hasAnyRole(Role.USER.name(), Role.ADMIN.name())

                                                .anyRequest().authenticated())

                                .formLogin(form -> form
                                                .loginPage("/login")
                                                .successHandler(authenticationSuccessHandler())
                                                .permitAll())
                                .logout(config -> config
                                                // Allow both GET and POST for logout to support links and forms
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
                                .csrf(csrf -> csrf.ignoringRequestMatchers("/logout"))
                                .build();
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        public AuthenticationSuccessHandler authenticationSuccessHandler() {
                return (HttpServletRequest request, HttpServletResponse response, Authentication authentication) -> {
                        // Check if user has ADMIN role
                        boolean isAdmin = authentication.getAuthorities().stream()
                                        .anyMatch(grantedAuthority -> grantedAuthority.getAuthority()
                                                        .equals("ROLE_ADMIN"));

                        if (isAdmin) {
                                response.sendRedirect("/admin/dashboard");
                        } else {
                                response.sendRedirect("/profile");
                        }
                };
        }

}
