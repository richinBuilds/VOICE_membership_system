package org.voice.membership.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.voice.membership.dtos.AdminAddMemberRequest;
import org.voice.membership.dtos.AdminUpdateUserRequest;
import org.voice.membership.dtos.BulkEmailRequest;
import org.voice.membership.entities.Membership;
import org.voice.membership.entities.Role;
import org.voice.membership.entities.User;
import org.voice.membership.repositories.*;

import java.util.Date;
import java.util.List;

/**
 * Service for admin member management operations.
 * Handles creating, updating, and deleting user accounts by administrators.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminMemberService {

    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final PasswordEncoder passwordEncoder;
    private final ChildRepository childRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final MembershipPaymentTransactionRepository paymentTransactionRepository;
    private final MembershipService membershipService;
    private final AdminNotificationService adminNotificationService;
    private final EmailSenderService emailSenderService;

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
                .city(memberRequest.getCity())
                .province(memberRequest.getProvince())
                .postalCode(memberRequest.getPostalCode())
                .chapter(memberRequest.getChapter())
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

                // Set membership dates for paid memberships
                if (!membership.isFree()) {
                    Date now = new Date();
                    newUser.setPaid(true);
                    newUser.setMembershipStartDate(now);
                    newUser.setMembershipExpiryDate(membershipService.calculateMembershipExpiry(now));
                }
            }
        }

        User savedUser = userRepository.save(newUser);

        // Send instant notification to admin for new member
        try {
            adminNotificationService.createInstantNotification(savedUser);
        } catch (Exception e) {
            log.error("Failed to create instant admin notification for user {}", savedUser.getId(), e);
        }

        return savedUser;
    }

    /**
     * Update an existing member's profile.
     * 
     * @param userId        the user ID to update
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
        user.setChapter(updateRequest.getChapter());

        // Track if membership is being changed
        Integer previousMembershipId = user.getMembership() != null ? user.getMembership().getId() : null;
        boolean membershipChanged = false;

        // Update membership if changed
        if (updateRequest.getMembershipId() != null) {
            Membership membership = membershipRepository.findById(updateRequest.getMembershipId()).orElse(null);
            user.setMembership(membership);

            // Set membership dates for paid memberships
            if (membership != null && !membership.isFree()) {
                Date now = new Date();
                user.setPaid(true);
                user.setMembershipStartDate(now);
                user.setMembershipExpiryDate(membershipService.calculateMembershipExpiry(now));
            } else if (membership != null && membership.isFree()) {
                // Clear dates for free membership
                user.setPaid(false);
                user.setMembershipStartDate(null);
                user.setMembershipExpiryDate(null);
            }

            // Check if membership actually changed
            if (!updateRequest.getMembershipId().equals(previousMembershipId)) {
                membershipChanged = true;
            }
        } else {
            user.setMembership(null);
            user.setPaid(false);
            user.setMembershipStartDate(null);
            user.setMembershipExpiryDate(null);

            // Membership was removed
            if (previousMembershipId != null) {
                membershipChanged = true;
            }
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
        User updatedUser = userRepository.save(user);

        // Send instant notification to admin if membership was changed
        if (membershipChanged) {
            try {
                adminNotificationService.createInstantNotification(updatedUser);
            } catch (Exception e) {
                log.error("Failed to create instant admin notification for user {}", updatedUser.getId(), e);
            }
        }

        return updatedUser;
    }

    /**
     * Delete a member account.
     * Prevents deletion of admin accounts.
     * 
     * @param userId the user ID to delete
     * @return the deleted user, or null if user not found or is admin
     */
    @Transactional
    public User deleteMember(Integer userId) {
        User user = userRepository.findById(userId).orElse(null);

        if (user == null) {
            return null; // User not found
        }

        // Prevent deleting admin accounts
        if (user.getRole() != null && user.getRole().equals(Role.ADMIN.name())) {
            return null; // Cannot delete admin
        }

        // Delete all related entities first to avoid foreign key constraint violations

        // 1. Delete children
        childRepository.deleteAll(childRepository.findByUser(user));

        // 2. Delete cart items first, then cart
        cartRepository.findByUser(user).ifPresent(cart -> {
            // Delete cart items using bulk delete to avoid orphan removal issues
            cartItemRepository.deleteByCartId(cart.getId());
            // Then delete the cart
            cartRepository.delete(cart);
        });

        // 3. Delete verification token
        verificationTokenRepository.findByUser(user).ifPresent(verificationTokenRepository::delete);

        // 4. Delete payment transactions
        paymentTransactionRepository.deleteByUser_Id(userId);

        // 5. Finally delete the user
        userRepository.delete(user);
        return user;
    }

    /**
     * Create a new admin account.
     * 
     * @param adminRequest the admin details
     * @return the created admin user, or null if email already exists
     */
    public User createAdmin(org.voice.membership.dtos.AdminAddAdminRequest adminRequest) {
        // Check if email already exists
        User existingUser = userRepository.findByEmail(adminRequest.getEmail());
        if (existingUser != null) {
            return null; // Email conflict
        }

        // Create new admin user
        User newAdmin = User.builder()
                .firstName(adminRequest.getFirstName())
                .middleName(adminRequest.getMiddleName())
                .lastName(adminRequest.getLastName())
                .email(adminRequest.getEmail())
                .password(passwordEncoder.encode(adminRequest.getPassword()))
                .phone("N/A")
                .address("N/A")
                .postalCode("N/A")
                .role(Role.ADMIN.name())
                .emailVerified(true)
                .accountLocked(false)
                .creation(new Date())
                .build();

        return userRepository.save(newAdmin);
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

    // -----------------------------------------------------------------------
    // Convenience lookups
    // -----------------------------------------------------------------------

    /**
     * Return the formatted full name of the admin with the given email.
     */
    public String getAdminNameByEmail(String email) {
        return formatAdminName(userRepository.findByEmail(email));
    }

    /**
     * Fetch a user by primary key, or {@code null} if not found.
     */
    public User getUserById(Integer id) {
        if (id == null) {
            return null;
        }
        return userRepository.findById(id).orElse(null);
    }

    /**
     * Return all users in the system.
     */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // -----------------------------------------------------------------------
    // Bulk email
    // -----------------------------------------------------------------------

    /**
     * Immutable result of a bulk-email send operation.
     */
    public record BulkEmailResult(int successCount, int failureCount, String message) {
    }

    /**
     * Send a custom email to every user in {@code request.getRecipientIds()}.
     *
     * @param request   the bulk email payload (recipients, subject, body)
     * @param adminName the sender name shown at the bottom of each email
     * @return a {@link BulkEmailResult} with per-send counts and a summary message
     */
    public BulkEmailResult sendBulkEmails(BulkEmailRequest request, String adminName) {
        int successCount = 0;
        int failureCount = 0;
        StringBuilder failedEmails = new StringBuilder();

        log.info("Starting bulk email send to {} recipients", request.getRecipientIds().size());

        for (Integer userId : request.getRecipientIds()) {
            try {
                User user = userRepository.findById(userId).orElse(null);
                if (user != null && user.getEmail() != null) {
                    log.debug("Sending email to user {} ({})", user.getId(), user.getEmail());
                    emailSenderService.sendCustomEmail(
                            user.getEmail(),
                            request.getSubject(),
                            request.getMessageBody(),
                            adminName);
                    successCount++;
                    log.debug("Email sent successfully to user {} ({})", user.getId(), user.getEmail());
                } else {
                    failureCount++;
                    log.warn("User not found or email missing for userId {}", userId);
                    if (user != null) {
                        failedEmails.append(user.getFirstName()).append(" ")
                                .append(user.getLastName()).append(", ");
                    }
                }
            } catch (Exception e) {
                failureCount++;
                log.error("Error sending email to userId {}: {}", userId, e.getMessage(), e);
                User user = userRepository.findById(userId).orElse(null);
                if (user != null) {
                    failedEmails.append(user.getEmail()).append(" (").append(e.getMessage()).append("), ");
                }
            }
        }

        log.info("Bulk email send completed: {} successful, {} failed", successCount, failureCount);
        String message = buildBulkEmailSummary(successCount, failureCount, failedEmails.toString());
        return new BulkEmailResult(successCount, failureCount, message);
    }

    private String buildBulkEmailSummary(int successCount, int failureCount, String failedEmailsStr) {
        String message = "Emails sent successfully to " + successCount + " recipient(s)";
        if (failureCount > 0) {
            message += ". Failed to send to " + failureCount + " recipient(s)";
            if (!failedEmailsStr.isBlank()) {
                String failed = failedEmailsStr.endsWith(", ")
                        ? failedEmailsStr.substring(0, failedEmailsStr.length() - 2)
                        : failedEmailsStr;
                message += ": " + failed;
            }
        }
        return message;
    }
}
