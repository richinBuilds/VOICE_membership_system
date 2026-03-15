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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Scheduled service that sends membership renewal reminder emails.
 *
 * Runs daily at 8:00 AM and checks for paid members whose membership
 * expiry date falls exactly 30, 14, or 7 days from today. A reminder
 * email is sent to each qualifying member so they can renew on time.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MembershipRenewalSchedulerService {

    private final UserRepository userRepository;
    private final EmailSenderService emailSenderService;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    /** Reminder intervals (in days before expiry) at which emails are sent. */
    private static final int[] REMINDER_DAYS = {30, 14, 7};

    /**
     * Scheduled task that fires every day at 8:00 AM.
     *
     * For each configured reminder interval the method:
     * <ol>
     *   <li>Calculates a 24-hour window starting at midnight of (today + N days).</li>
     *   <li>Queries for paid members whose expiry date falls within that window.</li>
     *   <li>Sends a personalised renewal reminder email to each such member.</li>
     * </ol>
     *
     * Failures for individual members are caught and logged so one bad address
     * cannot block reminders to the rest of the list.
     */
    /**
     * Scheduled task — fires every day at 8:00 AM.
     * Also called directly by the admin trigger endpoint.
     * Returns a summary map so callers can report results to the UI.
     */
    @Scheduled(cron = "0 0 8 * * ?") // Every day at 08:00 AM
    @Transactional(readOnly = true)
    public Map<String, Object> sendRenewalReminders() {
        log.info("Starting membership renewal reminder job");

        SimpleDateFormat dateFormatter  = new SimpleDateFormat("MMMM dd, yyyy");
        SimpleDateFormat windowFormatter = new SimpleDateFormat("yyyy-MM-dd");
        String renewalUrl = baseUrl + "/upgrade-membership";

        int totalFound  = 0;
        int totalSent   = 0;
        int totalFailed = 0;
        Map<String, Object> windowDetails = new LinkedHashMap<>();

        for (int days : REMINDER_DAYS) {
            // Build the 24-hour window for "today + N days"
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            cal.add(Calendar.DAY_OF_MONTH, days);
            Date windowStart = cal.getTime();

            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            cal.set(Calendar.SECOND, 59);
            cal.set(Calendar.MILLISECOND, 999);
            Date windowEnd = cal.getTime();

            List<User> expiringMembers = userRepository.findPaidMembersExpiringBetween(windowStart, windowEnd);
            log.info("[{}d window] Found {} paid member(s) expiring between {} and {}",
                    days, expiringMembers.size(),
                    windowFormatter.format(windowStart),
                    windowFormatter.format(windowEnd));

            int windowSent   = 0;
            int windowFailed = 0;
            List<String> sentTo   = new ArrayList<>();
            List<String> failedTo = new ArrayList<>();

            for (User user : expiringMembers) {
                try {
                    String membershipName = (user.getMembership() != null)
                            ? user.getMembership().getName()
                            : "Premium Membership";

                    emailSenderService.sendRenewalReminderEmail(
                            user.getEmail(),
                            user.getFirstName(),
                            membershipName,
                            dateFormatter.format(user.getMembershipExpiryDate()),
                            days,
                            renewalUrl
                    );

                    log.info("Sent {}-day renewal reminder to {}", days, user.getEmail());
                    sentTo.add(user.getEmail());
                    windowSent++;

                } catch (Exception e) {
                    log.error("Failed to send {}-day renewal reminder to {}: {}",
                            days, user.getEmail(), e.getMessage());
                    failedTo.add(user.getEmail() + " (" + e.getMessage() + ")");
                    windowFailed++;
                }
            }

            totalFound  += expiringMembers.size();
            totalSent   += windowSent;
            totalFailed += windowFailed;

            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("membersFound", expiringMembers.size());
            detail.put("emailsSent",   windowSent);
            detail.put("emailsFailed", windowFailed);
            detail.put("sentTo",   sentTo);
            detail.put("failedTo", failedTo);
            windowDetails.put(days + "-day window", detail);
        }

        log.info("Renewal reminder job done: {} found, {} sent, {} failed",
                totalFound, totalSent, totalFailed);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalMembersFound", totalFound);
        summary.put("totalEmailsSent",   totalSent);
        summary.put("totalEmailsFailed", totalFailed);
        summary.put("windows", windowDetails);
        return summary;
    }

    /**
     * Preview which paid members would receive a reminder within the next
     * {@code withinDays} days — does NOT send any emails.
     * Used by the admin preview endpoint so admins can verify their test data
     * before triggering.
     *
     * @param withinDays look-ahead horizon (1-365)
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> previewExpiringMembers(int withinDays) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date windowStart = cal.getTime();

        cal.add(Calendar.DAY_OF_MONTH, withinDays);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        Date windowEnd = cal.getTime();

        List<User> members = userRepository.findPaidMembersExpiringBetween(windowStart, windowEnd);
        log.info("Preview: {} paid member(s) expiring within {} day(s)", members.size(), withinDays);

        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
        List<Map<String, Object>> result = new ArrayList<>();
        Date now = new Date();

        for (User u : members) {
            long diffMs   = u.getMembershipExpiryDate().getTime() - now.getTime();
            long daysLeft = diffMs / (1000L * 60 * 60 * 24);

            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id",              u.getId());
            entry.put("name",            u.getFirstName() + " " + u.getLastName());
            entry.put("email",           u.getEmail());
            entry.put("membership",      u.getMembership() != null ? u.getMembership().getName() : "N/A");
            entry.put("expiryDate",      fmt.format(u.getMembershipExpiryDate()));
            entry.put("daysUntilExpiry", daysLeft);
            entry.put("paid",            u.isPaid());
            result.add(entry);
        }
        return result;
    }
}
