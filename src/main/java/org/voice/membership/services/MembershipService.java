package org.voice.membership.services;

import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Service for membership-related business logic.
 * Handles membership expiry calculations, date formatting, and membership
 * lifecycle operations.
 */
@Service
public class MembershipService {

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
}
