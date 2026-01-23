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

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        return httpSecurity
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/").permitAll()
                        .requestMatchers("/login").permitAll()
                        .requestMatchers("/register").permitAll()
                        .requestMatchers("/forgot-password").permitAll()
                        .requestMatchers("/reset-password").permitAll()
                        .requestMatchers("/api/landing-page/**").permitAll()
                        .requestMatchers("/profile").hasRole(Role.USER.name())
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/profile", true))
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

}
