package org.voice.membership.services;

import org.voice.membership.entities.User;
import org.voice.membership.repositories.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
public class UserService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    public UserDetails loadUserByUsername(String email) {
        User user = userRepository.findByEmail(email);
        if (user != null) {
            return org.springframework.security.core.userdetails.User
                    .withUsername(user.getEmail())
                    .password(user.getPassword())
                    .roles(user.getRole())
                    .build();
        }
        throw new UsernameNotFoundException("User not found with email: " + email);
    }

    @Autowired(required = false)
    private EmailSenderService emailSenderService;

    // In-memory token storage for demo (token -> user email)
    private final Map<String, String> resetTokens = new ConcurrentHashMap<>();

    public boolean sendPasswordResetEmail(String email) {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            return false;
        }
        String token = java.util.UUID.randomUUID().toString();
        resetTokens.put(token, user.getEmail());
        String resetLink = "http://localhost:8080/reset-password?token=" + token;
        if (emailSenderService != null) {
            emailSenderService.sendPasswordResetEmail(user.getEmail(), resetLink);
        }
        return true;
    }

    @Autowired
    private PasswordEncoder passwordEncoder;

    public boolean resetPassword(String token, String newPassword) {
        String email = resetTokens.get(token);
        if (email == null) {
            return false;
        }
        User user = userRepository.findByEmail(email);
        if (user == null) {
            return false;
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        resetTokens.remove(token);
        return true;
    }
}