package org.voice.membership.services;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.voice.membership.dtos.AdminAddMemberRequest;
import org.voice.membership.dtos.AdminUpdateUserRequest;
import org.voice.membership.entities.Membership;
import org.voice.membership.entities.Role;
import org.voice.membership.entities.User;
import org.voice.membership.repositories.MembershipRepository;
import org.voice.membership.repositories.UserRepository;

import java.util.Date;
import java.util.List;

/**
 * Service for admin member management operations.
 * Handles creating, updating, and deleting user accounts by administrators.
 */
@Service
@RequiredArgsConstructor
public class AdminMemberService {

    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Create a new member account.
     * 
     * @param memberRequest the member details
     * @return the created user, or null if email already exists
     */
    public User createMember(AdminAddMemberRequest memberRequest) {
        // Check if email already exists
        User existingUser = userRepository.findByEmail(memberRequest.getEmail());
        if (existingUser != null) {
            return null; // Email conflict
        }

        // Create new user
        User newUser = User.builder()
                .firstName(memberRequest.getFirstName())
                .middleName(memberRequest.getMiddleName())
                .lastName(memberRequest.getLastName())
                .email(memberRequest.getEmail())
                .password(passwordEncoder.encode(memberRequest.getPassword()))
                .phone(memberRequest.getPhone())
                .address(memberRequest.getAddress())
                .postalCode(memberRequest.getPostalCode())
                .role(Role.USER.name())
                .emailVerified(memberRequest.getEmailVerified())
                .accountLocked(memberRequest.getAccountLocked())
                .creation(new Date())
                .build();

        // Assign membership if selected
        if (memberRequest.getMembershipId() != null) {
            Membership membership = membershipRepository.findById(memberRequest.getMembershipId()).orElse(null);
            if (membership != null) {
                newUser.setMembership(membership);
            }
        }

        return userRepository.save(newUser);
    }

    /**
     * Update an existing member's profile.
     * 
     * @param userId the user ID to update
     * @param updateRequest the updated member details
     * @return the updated user, or null if user not found or email conflict
     */
    public User updateMember(Integer userId, AdminUpdateUserRequest updateRequest) {
        // Load the user to be edited
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return null; // User not found
        }

        // Check if email is being changed and if it conflicts with another user
        if (!user.getEmail().equalsIgnoreCase(updateRequest.getEmail())) {
            List<User> usersWithEmail = userRepository.findAllByEmailIgnoreCase(updateRequest.getEmail());
            boolean emailConflict = usersWithEmail.stream().anyMatch(u -> u.getId() != user.getId());

            if (emailConflict) {
                return null; // Email conflict
            }
        }

        // Update user information
        user.setFirstName(updateRequest.getFirstName());
        user.setMiddleName(updateRequest.getMiddleName());
        user.setLastName(updateRequest.getLastName());
        user.setEmail(updateRequest.getEmail());
        user.setPhone(updateRequest.getPhone());
        user.setAddress(updateRequest.getAddress());
        user.setCity(updateRequest.getCity());
        user.setProvince(updateRequest.getProvince());
        user.setPostalCode(updateRequest.getPostalCode());

        // Update membership if changed
        if (updateRequest.getMembershipId() != null) {
            Membership membership = membershipRepository.findById(updateRequest.getMembershipId()).orElse(null);
            user.setMembership(membership);
        } else {
            user.setMembership(null);
        }

        // Update email verification status
        if (updateRequest.getEmailVerified() != null) {
            user.setEmailVerified(updateRequest.getEmailVerified());
        }

        // Update account locked status
        if (updateRequest.getAccountLocked() != null) {
            user.setAccountLocked(updateRequest.getAccountLocked());
            if (!updateRequest.getAccountLocked()) {
                // If unlocking account, reset failed attempts
                user.setFailedLoginAttempts(0);
                user.setLockoutTime(null);
            }
        }

        // Save the updated user
        return userRepository.save(user);
    }

    /**
     * Delete a member account.
     * Prevents deletion of admin accounts.
     * 
     * @param userId the user ID to delete
     * @return the deleted user, or null if user not found or is admin
     */
    public User deleteMember(Integer userId) {
        User user = userRepository.findById(userId).orElse(null);

        if (user == null) {
            return null; // User not found
        }

        // Prevent deleting admin accounts
        if (user.getRole() != null && user.getRole().equals(Role.ADMIN.name())) {
            return null; // Cannot delete admin
        }

        userRepository.delete(user);
        return user;
    }

    /**
     * Format admin user's full name for display.
     * Includes middle name if present.
     * 
     * @param admin the admin user
     * @return formatted full name
     */
    public String formatAdminName(User admin) {
        if (admin == null) {
            return "Admin";
        }

        StringBuilder name = new StringBuilder();
        name.append(admin.getFirstName());

        if (admin.getMiddleName() != null && !admin.getMiddleName().isEmpty()) {
            name.append(" ").append(admin.getMiddleName());
        }

        name.append(" ").append(admin.getLastName());

        return name.toString();
    }
}
