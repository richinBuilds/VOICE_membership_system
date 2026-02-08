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
import org.springframework.beans.factory.annotation.Autowired;

@Configuration
@EnableWebSecurity
/**
 * Configures Spring Security for the VOICE membership application.
 * Defines public and protected routes, login/logout, remember-me, and
 * redirects.
 */
public class SecurityConfig {

        @Autowired
        private CustomAuthenticationFailureHandler authenticationFailureHandler;

        @Autowired
        private CustomAuthenticationSuccessHandler authenticationSuccessHandler;

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
                return httpSecurity
                                .authorizeHttpRequests(auth -> auth

                                                .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()
                                                .requestMatchers("/").permitAll()
                                                .requestMatchers("/login").permitAll()
                                                .requestMatchers("/register/**").permitAll()
                                                .requestMatchers("/forgot-password").permitAll()
                                                .requestMatchers("/reset-password").permitAll()
                                                .requestMatchers("/api/landing-page/**").permitAll()

                                                .requestMatchers("/admin/**").hasRole(Role.ADMIN.name())

                                                .requestMatchers("/profile/**")
                                                .hasAnyRole(Role.USER.name(), Role.ADMIN.name())

                                                .anyRequest().authenticated())

                                .formLogin(form -> form
                                                .loginPage("/login")
                                                .successHandler(authenticationSuccessHandler)
                                                .failureHandler(authenticationFailureHandler)
                                                .permitAll())
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
                                .csrf(csrf -> csrf.ignoringRequestMatchers("/logout"))
                                .build();
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

}
