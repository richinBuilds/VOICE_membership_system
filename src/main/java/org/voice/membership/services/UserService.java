package org.voice.membership.services;

import org.voice.membership.dtos.UpdateUserRequest;
import org.voice.membership.entities.User;
import org.voice.membership.repositories.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
/**
 * Provides user-related operations and Spring Security user lookup.
 * Handles loading users for authentication and password reset workflows.
 */
public class UserService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountLockoutService accountLockoutService;

    @Autowired
    private MembershipService membershipService;

    public UserDetails loadUserByUsername(String email) {
        User user = userRepository.findByEmail(email);
        if (user != null) {
            // Check and downgrade expired memberships automatically
            membershipService.downgradeExpiredMembership(user);

            // Check if account is locked
            if (accountLockoutService.isAccountLocked(email)) {
                long remainingMinutes = accountLockoutService.getRemainingLockoutTime(email);
                throw new org.springframework.security.authentication.LockedException(
                        "Your account has been temporarily locked due to multiple failed login attempts. " +
                                "Please try again in " + remainingMinutes + " minute(s).");
            }

            // Check if email is verified
            if (!user.isEmailVerified()) {
                throw new org.springframework.security.authentication.DisabledException(
                        "Please verify your email before logging in. Check your inbox for the verification link.");
            }

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

    @Value("${app.base-url:http://localhost:8080}")
    private String appBaseUrl;

    private final Map<String, String> resetTokens = new ConcurrentHashMap<>();

    public boolean sendPasswordResetEmail(String email) {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            return false;
        }
        String token = java.util.UUID.randomUUID().toString();
        resetTokens.put(token, user.getEmail());
        String resetLink = appBaseUrl + "/reset-password?token=" + token;
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

    /**
     * Update user profile information.
     * Handles email conflict checking and updates all user fields.
     * 
     * @param currentEmail  The user's current email (for lookup)
     * @param updateRequest The update request with new user information
     * @return The updated user, or null if email conflict exists
     */
    public User updateProfile(String currentEmail, UpdateUserRequest updateRequest) {
        User user = userRepository.findByEmail(currentEmail);
        if (user == null) {
            return null;
        }

        // Update basic info
        user.setFirstName(updateRequest.getFirstName());
        user.setMiddleName(updateRequest.getMiddleName());
        user.setLastName(updateRequest.getLastName());

        // Check email conflict if email is being changed
        String newEmail = updateRequest.getEmail();
        if (newEmail != null && !newEmail.equalsIgnoreCase(currentEmail)) {
            List<User> matches = userRepository.findAllByEmailIgnoreCase(newEmail);
            boolean conflict = matches.stream().anyMatch(u -> u.getId() != user.getId());
            if (conflict) {
                return null; // Email conflict
            }
            user.setEmail(newEmail);
        }

        // Update contact and address info
        user.setPhone(updateRequest.getPhone());
        user.setAddress(updateRequest.getAddress());
        user.setCity(updateRequest.getCity());
        user.setProvince(updateRequest.getProvince());
        user.setPostalCode(updateRequest.getPostalCode());

        return userRepository.save(user);
    }
}