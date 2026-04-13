package org.voice.membership.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.voice.membership.entities.Membership;
import org.voice.membership.entities.User;
import org.voice.membership.repositories.MembershipRepository;
import org.voice.membership.repositories.UserRepository;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Service for membership-related business logic.
 * Handles membership expiry calculations, date formatting, and membership
 * lifecycle operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MembershipService {

    private final MembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final AdminNotificationService adminNotificationService;

    /**
     * Calculate membership expiry date from a start date.
     * Currently adds 1 year to the start date.
     * 
     * @param startDate The membership start date
     * @return The expiry date (1 year after start date)
     */
    public Date calculateMembershipExpiry(Date startDate) {
        if (startDate == null) {
            return null;
        }

        Calendar cal = Calendar.getInstance();
        cal.setTime(startDate);
        cal.add(Calendar.YEAR, 1);
        return cal.getTime();
    }

    /**
     * Format a date as a human-readable membership expiry string.
     * 
     * @param date The date to format
     * @return Formatted date string (e.g., "January 15, 2025")
     */
    public String formatMembershipDate(Date date) {
        if (date == null) {
            return "-";
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM dd, yyyy");
        return dateFormat.format(date);
    }

    /**
     * Check if a membership has expired.
     * 
     * @param expiryDate The membership expiry date
     * @return true if expired, false otherwise
     */
    public boolean isMembershipExpired(Date expiryDate) {
        if (expiryDate == null) {
            return false;
        }

        return new Date().after(expiryDate);
    }

    /**
     * Check if user's paid membership has expired and downgrade to free if needed.
     * This method should be called when a user logs in or accesses their profile.
     * 
     * @param user The user to check and potentially downgrade
     * @return true if downgrade occurred, false otherwise
     */
    public boolean downgradeExpiredMembership(User user) {
        if (user == null || user.getMembership() == null) {
            return false;
        }

        Membership currentMembership = user.getMembership();

        // Only check paid memberships
        if (currentMembership.isFree()) {
            return false;
        }

        // Check if membership has expired
        Date expiryDate = user.getMembershipExpiryDate();
        if (expiryDate != null && isMembershipExpired(expiryDate)) {
            // Get the free membership
            List<Membership> freeMemberships = membershipRepository.findByIsFree(true);
            if (!freeMemberships.isEmpty()) {
                Membership freeMembership = freeMemberships.get(0);

                // Downgrade to free membership
                user.setMembership(freeMembership);
                user.setPaid(false);
                user.setMembershipExpiryDate(null);
                user.setMembershipStartDate(null);

                // Save the changes
                userRepository.save(user);

                return true;
            }
        }

        return false;
    }

    // -----------------------------------------------------------------------
    // Membership retrieval helpers
    // -----------------------------------------------------------------------

    /**
     * Find a membership by its primary key.
     */
    public Optional<Membership> getMembershipById(Integer id) {
        if (id == null) {
            return Optional.empty();
        }
        return membershipRepository.findById(id);
    }

    /**
     * Return all active memberships ordered by display order (ascending).
     */
    public List<Membership> getActiveMembershipsOrdered() {
        return membershipRepository.findByActiveTrueOrderByDisplayOrderAsc();
    }

    /**
     * Return all active memberships.
     */
    public List<Membership> getActiveMemberships() {
        return membershipRepository.findByActiveTrue();
    }

    /**
     * Return every membership regardless of active status.
     */
    public List<Membership> getAllMemberships() {
        return membershipRepository.findAll();
    }

    /**
     * Return all paid (non-free) memberships.
     */
    public List<Membership> getPaidMemberships() {
        return membershipRepository.findByIsFree(false);
    }

    // -----------------------------------------------------------------------
    // Membership upgrade
    // -----------------------------------------------------------------------

    /**
     * Apply a paid membership upgrade to a user: set the membership, update
     * start/expiry dates, persist the user, and notify the admin.
     *
     * @param user           the user being upgraded
     * @param paidMembership the new paid membership to assign
     */
    public void applyMembershipUpgrade(User user, Membership paidMembership) {
        user.setMembership(paidMembership);
        user.setPaid(true);
        Date now = new Date();
        user.setMembershipStartDate(now);
        user.setMembershipExpiryDate(calculateMembershipExpiry(now));
        userRepository.save(user);
        try {
            adminNotificationService.createInstantNotification(user);
        } catch (Exception e) {
            log.error("Failed to create admin notification for user {}", user.getId(), e);
        }
    }

    // -----------------------------------------------------------------------
    // Admin membership editor
    // -----------------------------------------------------------------------

    /**
     * Update a membership plan's name, description, price, and features.
     *
     * @param id          primary key of the membership
     * @param name        new display name
     * @param description new description
     * @param price       new price string (ignored for free memberships or when
     *                    blank)
     * @param features    newline-separated features text (browser line endings are
     *                    normalised to the system separator)
     * @return the saved Membership, or {@code null} if the id was not found
     * @throws NumberFormatException if {@code price} is non-blank but not a valid
     *                               decimal number
     */
    public Membership updateMembership(int id, String name, String description,
            String price, String features) {
        Membership membership = membershipRepository.findById(id).orElse(null);
        if (membership == null) {
            return null;
        }
        membership.setName(name.trim());
        membership.setDescription(description.trim());
        if (price != null && !price.trim().isEmpty()) {
            membership.setPrice(new BigDecimal(price.trim()));
        }
        if (features != null) {
            String normalised = features.replace("\r\n", "\n").replace("\r", "\n");
            normalised = normalised.replace("\n", System.lineSeparator());
            membership.setFeatures(normalised);
        }
        return membershipRepository.save(membership);
    }
}
