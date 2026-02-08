package org.voice.membership.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.voice.membership.entities.User;
import org.voice.membership.entities.Membership;
import org.voice.membership.repositories.UserRepository;
import org.voice.membership.repositories.MembershipRepository;

import java.util.Date;
import java.util.Optional;

/**
 * Service responsible for handling membership cancellation operations.
 * Allows members to cancel their own memberships with proper validation.
 */
@Service
public class MembershipCancellationService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MembershipRepository membershipRepository;

    /**
     * Cancels a user's membership by setting it to null or a free membership.
     * Only paid memberships can be cancelled.
     * 
     * @param userId The ID of the user whose membership should be cancelled
     * @return CancellationResult containing success status and message
     */
    @Transactional
    public CancellationResult cancelMembership(int userId) {
        User user = userRepository.findById(userId).orElse(null);

        if (user == null) {
            return new CancellationResult(false, "User not found");
        }

        Membership currentMembership = user.getMembership();

        if (currentMembership == null) {
            return new CancellationResult(false, "No active membership to cancel");
        }

        // Only allow cancellation of paid memberships
        if (currentMembership.isFree()) {
            return new CancellationResult(false,
                    "Free memberships cannot be cancelled. You already have a free membership.");
        }

        // Store previous membership info for logging/audit purposes
        String cancelledMembershipName = currentMembership.getName();

        // Find free membership option to assign to user
        Optional<Membership> freeMembershipOpt = membershipRepository.findByIsFree(true)
                .stream()
                .findFirst();

        if (freeMembershipOpt.isPresent()) {
            // Downgrade to free membership
            user.setMembership(freeMembershipOpt.get());
            user.setMembershipStartDate(new Date());
            user.setMembershipExpiryDate(null); // Free membership doesn't expire
        } else {
            // If no free membership exists, set to null
            user.setMembership(null);
            user.setMembershipStartDate(null);
            user.setMembershipExpiryDate(null);
        }

        userRepository.save(user);

        return new CancellationResult(true,
                String.format("Successfully cancelled %s membership", cancelledMembershipName));
    }

    /**
     * Checks if a user can cancel their membership.
     * Only paid memberships can be cancelled, not free memberships.
     * 
     * @param userId The ID of the user
     * @return true if the user has an active paid membership that can be cancelled
     */
    public boolean canCancelMembership(int userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || user.getMembership() == null) {
            return false;
        }
        // Only allow cancellation of paid memberships
        return !user.getMembership().isFree();
    }

    /**
     * Gets information about the current membership that would be cancelled.
     * 
     * @param userId The ID of the user
     * @return MembershipInfo containing details about the current membership
     */
    public MembershipInfo getCurrentMembershipInfo(int userId) {
        User user = userRepository.findById(userId).orElse(null);

        if (user == null || user.getMembership() == null) {
            return new MembershipInfo(null, false, "No membership");
        }

        Membership membership = user.getMembership();
        return new MembershipInfo(
                membership.getName(),
                membership.isFree(),
                membership.getDescription());
    }

    /**
     * Result of a membership cancellation operation.
     */
    public static class CancellationResult {
        private final boolean success;
        private final String message;

        public CancellationResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }

    /**
     * Information about a membership.
     */
    public static class MembershipInfo {
        private final String name;
        private final boolean isFree;
        private final String description;

        public MembershipInfo(String name, boolean isFree, String description) {
            this.name = name;
            this.isFree = isFree;
            this.description = description;
        }

        public String getName() {
            return name;
        }

        public boolean isFree() {
            return isFree;
        }

        public String getDescription() {
            return description;
        }
    }
}
