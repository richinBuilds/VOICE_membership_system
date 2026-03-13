package org.voice.membership.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.voice.membership.entities.User;
import org.voice.membership.repositories.UserRepository;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Service for sending membership renewal reminder emails to paid members.
 * Automatically checks for expiring memberships and sends reminder emails.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MembershipRenewalReminderService {

    private final UserRepository userRepository;
    private final EmailSenderService emailSenderService;
    private final MembershipService membershipService;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    /**
     * Send renewal reminder emails to members whose memberships are expiring soon.
     * Runs daily at 11:04 AM to check for upcoming expirations.
     */
    @Scheduled(cron = "0 4 11 * * ?") // Run at 11:04 AM every day
    @Transactional(readOnly = true)
    public void sendRenewalReminders() {
        log.info("Starting membership renewal reminder check...");

        try {
            // Check for memberships expiring in 3 days
            List<User> usersExpiring3Days = findMembersExpiringInDays(3);
            sendRemindersToUsers(usersExpiring3Days, 3);

            log.info("Membership renewal reminder check completed successfully");
        } catch (Exception e) {
            log.error("Error during membership renewal reminder check", e);
        }
    }

    /**
     * Find members whose membership expires in the specified number of days.
     * Only includes paid members with valid expiry dates.
     *
     * @param daysUntilExpiry Number of days until expiry
     * @return List of users whose membership expires in the specified days
     */
    @Transactional(readOnly = true)
    public List<User> findMembersExpiringInDays(int daysUntilExpiry) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.add(Calendar.DAY_OF_MONTH, daysUntilExpiry);
        Date targetDate = cal.getTime();

        Calendar calEnd = Calendar.getInstance();
        calEnd.setTime(targetDate);
        calEnd.set(Calendar.HOUR_OF_DAY, 23);
        calEnd.set(Calendar.MINUTE, 59);
        calEnd.set(Calendar.SECOND, 59);
        calEnd.set(Calendar.MILLISECOND, 999);
        Date targetDateEnd = calEnd.getTime();

        return userRepository.findMembersExpiringBetween(targetDate, targetDateEnd);
    }

    /**
     * Send renewal reminder emails to a list of users.
     *
     * @param users           List of users to send reminders to
     * @param daysUntilExpiry Number of days until their membership expires
     */
    private void sendRemindersToUsers(List<User> users, int daysUntilExpiry) {
        if (users.isEmpty()) {
            log.info("No members found with memberships expiring in {} days", daysUntilExpiry);
            return;
        }

        log.info("Found {} members with memberships expiring in {} days", users.size(), daysUntilExpiry);

        int successCount = 0;
        int failureCount = 0;

        for (User user : users) {
            try {
                sendRenewalReminderEmail(user, daysUntilExpiry);
                successCount++;
                log.debug("Sent renewal reminder to: {}", user.getEmail());
            } catch (Exception e) {
                failureCount++;
                log.error("Failed to send renewal reminder to {}: {}", user.getEmail(), e.getMessage());
            }
        }

        log.info("Renewal reminders sent: {} successful, {} failed (for {} days until expiry)",
                successCount, failureCount, daysUntilExpiry);
    }

    /**
     * Send a renewal reminder email to a specific user.
     *
     * @param user            The user to send the reminder to
     * @param daysUntilExpiry Number of days until their membership expires
     */
    public void sendRenewalReminderEmail(User user, int daysUntilExpiry) {
        if (user == null || user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            log.warn("Cannot send renewal reminder: user or email is null");
            return;
        }

        if (user.getMembershipExpiryDate() == null) {
            log.warn("Cannot send renewal reminder to {}: no expiry date set", user.getEmail());
            return;
        }

        String userName = user.getFirstName() != null ? user.getFirstName() : "Member";
        String membershipName = user.getMembership() != null ? user.getMembership().getName() : "Your membership";
        String expiryDate = membershipService.formatMembershipDate(user.getMembershipExpiryDate());
        String renewalUrl = baseUrl + "/upgrade-membership";

        try {
            emailSenderService.sendMembershipRenewalReminder(
                    user.getEmail(),
                    userName,
                    membershipName,
                    expiryDate,
                    daysUntilExpiry,
                    renewalUrl);
            log.info("Successfully sent renewal reminder to {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send renewal reminder email to {}", user.getEmail(), e);
            throw new RuntimeException("Failed to send renewal reminder email", e);
        }
    }

    /**
     * Manually trigger renewal reminder check (useful for testing).
     * This method can be called via an admin endpoint for immediate testing.
     */
    @Transactional(readOnly = true)
    public void triggerManualReminderCheck() {
        log.info("Manual renewal reminder check triggered");
        sendRenewalReminders();
    }

    /**
     * Get count of members whose membership expires in the specified number of
     * days.
     *
     * @param daysUntilExpiry Number of days until expiry
     * @return Count of members expiring in the specified days
     */
    @Transactional(readOnly = true)
    public long countMembersExpiringInDays(int daysUntilExpiry) {
        List<User> users = findMembersExpiringInDays(daysUntilExpiry);
        return users.size();
    }
}
