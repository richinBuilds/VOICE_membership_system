package org.voice.membership.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.voice.membership.entities.Membership;
import org.voice.membership.entities.User;
import org.voice.membership.repositories.MembershipRepository;
import org.voice.membership.repositories.UserRepository;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Service for membership-related business logic.
 * Handles membership expiry calculations, date formatting, and membership
 * lifecycle operations.
 */
@Service
@RequiredArgsConstructor
public class MembershipService {

    private final MembershipRepository membershipRepository;
    private final UserRepository userRepository;

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
}
