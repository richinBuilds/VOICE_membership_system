package org.voice.membership.services;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.voice.membership.entities.AdminNotification;
import org.voice.membership.entities.User;
import org.voice.membership.repositories.AdminNotificationRepository;
import org.voice.membership.repositories.UserRepository;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Service for managing admin notifications about new paid members.
 * Generates daily and weekly notifications using scheduled tasks.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminNotificationService {

    private final AdminNotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Generate daily notification for new paid members.
     * Runs every day at 9:00 AM
     */
    @Scheduled(cron = "0 0 9 * * ?") // Run at 9:00 AM every day
    @Transactional
    public void generateDailyNotification() {
        log.info("Generating daily notification for new paid members");

        Calendar cal = Calendar.getInstance();
        Date periodEnd = cal.getTime();

        // Set to start of previous day
        cal.add(Calendar.DAY_OF_MONTH, -1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date periodStart = cal.getTime();

        generateNotification("DAILY", periodStart, periodEnd);
    }

    /**
     * Generate weekly notification for new paid members.
     * Runs every Monday at 9:00 AM
     */
    @Scheduled(cron = "0 0 9 ? * MON") // Run at 9:00 AM every Monday
    @Transactional
    public void generateWeeklyNotification() {
        log.info("Generating weekly notification for new paid members");

        Calendar cal = Calendar.getInstance();
        Date periodEnd = cal.getTime();

        // Set to start of previous week (Monday)
        cal.add(Calendar.WEEK_OF_YEAR, -1);
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date periodStart = cal.getTime();

        generateNotification("WEEKLY", periodStart, periodEnd);
    }

    /**
     * Create an instant notification when a new member joins.
     * This is called immediately when a user registers, when payment is processed,
     * or when admin creates a member.
     *
     * @param user The new member (paid or unpaid)
     */
    @Transactional
    public void createInstantNotification(User user) {
        if (user == null) {
            log.warn("Cannot create instant notification: user is null");
            return;
        }

        String userName = user.getFirstName() + " " + user.getLastName();
        String membershipType = user.isPaid() ? "paid" : "free";
        String membershipName = user.getMembership() != null ? user.getMembership().getName() : "No membership";
        String message = String.format("New %s member: %s joined with %s membership",
                membershipType, userName, membershipName);

        // Anchor instant notification window around user creation time.
        // This avoids Docker/DB timestamp precision and latency edge cases.
        Date anchor = user.getCreation() != null ? user.getCreation() : new Date();
        Calendar startCal = Calendar.getInstance();
        startCal.setTime(anchor);
        startCal.add(Calendar.MINUTE, -1);
        Date periodStart = startCal.getTime();

        Calendar endCal = Calendar.getInstance();
        endCal.setTime(anchor);
        endCal.add(Calendar.MINUTE, 1);
        Date periodEnd = endCal.getTime();

        AdminNotification notification = AdminNotification.builder()
                .message(message)
                .notificationType("INSTANT")
                .newMembersCount(1)
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .read(false)
                .dismissed(false)
                .build();

        notificationRepository.save(notification);
        log.info("Created instant notification for new {} member: {} ({})", membershipType, userName, user.getEmail());
    }

    /**
     * Generate notification for a specific period.
     * This method can also be called manually for testing or on-demand
     * notifications.
     *
     * @param notificationType DAILY or WEEKLY
     * @param periodStart      Start of the period
     * @param periodEnd        End of the period
     */
    @Transactional
    public AdminNotification generateNotification(String notificationType, Date periodStart, Date periodEnd) {
        // Check if notification already exists for this period
        if (notificationRepository.existsByNotificationTypeAndPeriodStartAndPeriodEnd(
                notificationType, periodStart, periodEnd)) {
            log.info("Notification already exists for period {} to {}", periodStart, periodEnd);
            return null;
        }

        // Find new paid members in the period
        List<User> newPaidMembers = userRepository.findNewPaidMembersBetweenDates(periodStart, periodEnd);

        int count = newPaidMembers.size();

        // Create notification message
        String message = generateNotificationMessage(notificationType, count, periodStart, periodEnd);

        // Create and save notification
        AdminNotification notification = AdminNotification.builder()
                .message(message)
                .notificationType(notificationType)
                .newMembersCount(count)
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .read(false)
                .dismissed(false)
                .build();

        notification = notificationRepository.save(notification);
        log.info("Created {} notification: {} new paid members", notificationType, count);

        return notification;
    }

    /**
     * Generate the notification message text
     */
    private String generateNotificationMessage(String notificationType, int count, Date periodStart, Date periodEnd) {
        if (count == 0) {
            return String.format("No new paid members in the past %s", notificationType.toLowerCase());
        } else if (count == 1) {
            return String.format("1 new paid member registered in the past %s", notificationType.toLowerCase());
        } else {
            return String.format("%d new paid members registered in the past %s", count,
                    notificationType.toLowerCase());
        }
    }

    /**
     * Get all unread notifications
     */
    @Transactional(readOnly = true)
    public List<AdminNotification> getUnreadNotifications() {
        return notificationRepository.findByReadFalseAndDismissedFalseOrderByCreatedAtDesc();
    }

    /**
     * Get all notifications
     */
    @Transactional(readOnly = true)
    public List<AdminNotification> getAllNotifications() {
        return notificationRepository.findAllByOrderByCreatedAtDesc();
    }

    /**
     * Get count of unread notifications
     */
    @Transactional(readOnly = true)
    public long getUnreadCount() {
        return notificationRepository.countByReadFalseAndDismissedFalse();
    }

    /**
     * Mark a notification as read
     */
    @Transactional
    public void markAsRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            notification.setRead(true);
            notificationRepository.save(notification);
            log.info("Marked notification {} as read", notificationId);
        });
    }

    /**
     * Mark all notifications as read
     */
    @Transactional
    public void markAllAsRead() {
        List<AdminNotification> unreadNotifications = notificationRepository
                .findByReadFalseAndDismissedFalseOrderByCreatedAtDesc();
        unreadNotifications.forEach(notification -> notification.setRead(true));
        notificationRepository.saveAll(unreadNotifications);
        log.info("Marked {} notifications as read", unreadNotifications.size());
    }

    /**
     * Dismiss a notification (remove from view)
     */
    @Transactional
    public void dismissNotification(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            notification.setDismissed(true);
            notificationRepository.save(notification);
            log.info("Dismissed notification {}", notificationId);
        });
    }

    /**
     * Dismiss all notifications
     */
    @Transactional
    public void dismissAllNotifications() {
        try {
            // Get all non-dismissed notifications (regardless of read status)
            List<AdminNotification> notifications = notificationRepository
                    .findByDismissedFalseOrderByCreatedAtDesc();

            log.info("Found {} non-dismissed notifications to dismiss", notifications.size());

            if (notifications.isEmpty()) {
                log.info("No notifications to dismiss");
                return;
            }

            notifications.forEach(notification -> {
                log.debug("Dismissing notification ID: {}", notification.getId());
                notification.setDismissed(true);
            });

            notificationRepository.saveAll(notifications);
            entityManager.flush(); // Force immediate database update
            log.info("Successfully dismissed {} notifications", notifications.size());
        } catch (Exception e) {
            log.error("Error dismissing all notifications: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Get notification details by ID
     */
    @Transactional(readOnly = true)
    public AdminNotification getNotificationById(Long notificationId) {
        return notificationRepository.findById(notificationId).orElse(null);
    }

    /**
     * Get members for a notification period.
     * INSTANT notifications include both paid and free users,
     * while DAILY/WEEKLY include paid users only.
     */
    @Transactional(readOnly = true)
    public List<User> getNewPaidMembersForNotification(Long notificationId) {
        AdminNotification notification = notificationRepository.findById(notificationId).orElse(null);
        if (notification == null) {
            return List.of();
        }

        if ("INSTANT".equalsIgnoreCase(notification.getNotificationType())) {
            List<User> users = userRepository.findNewMembersBetweenDates(
                    notification.getPeriodStart(),
                    notification.getPeriodEnd());

            // Backward compatibility for old INSTANT notifications that were stored
            // with an overly narrow period in production/Docker.
            if (users.isEmpty()) {
                Calendar fallbackStartCal = Calendar.getInstance();
                fallbackStartCal.setTime(notification.getCreatedAt());
                fallbackStartCal.add(Calendar.MINUTE, -5);

                Calendar fallbackEndCal = Calendar.getInstance();
                fallbackEndCal.setTime(notification.getCreatedAt());
                fallbackEndCal.add(Calendar.MINUTE, 5);

                users = userRepository.findNewMembersBetweenDates(
                        fallbackStartCal.getTime(),
                        fallbackEndCal.getTime());
            }

            return users;
        }

        return userRepository.findNewPaidMembersBetweenDates(
                notification.getPeriodStart(),
                notification.getPeriodEnd());
    }

    /**
     * Get all new users from all non-dismissed notifications
     * (includes both read and unread notifications)
     */
    @Transactional(readOnly = true)
    public List<User> getAllNewUsersFromNotifications() {
        // Get all non-dismissed notifications (not just unread ones)
        List<AdminNotification> notifications = notificationRepository.findByDismissedFalseOrderByCreatedAtDesc();
        if (notifications.isEmpty()) {
            return List.of();
        }

        // Get the earliest period start and latest period end from all notifications
        Date earliestStart = notifications.stream()
                .map(AdminNotification::getPeriodStart)
                .min(Date::compareTo)
                .orElse(new Date());

        Date latestEnd = notifications.stream()
                .map(AdminNotification::getPeriodEnd)
                .max(Date::compareTo)
                .orElse(new Date());

        return userRepository.findNewMembersBetweenDates(earliestStart, latestEnd);
    }
}
