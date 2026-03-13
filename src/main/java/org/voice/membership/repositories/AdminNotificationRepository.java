package org.voice.membership.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.voice.membership.entities.AdminNotification;

import java.util.Date;
import java.util.List;

/**
 * Repository for managing admin notifications.
 * Provides queries for fetching unread notifications and filtering by date
 * range.
 */
@Repository
public interface AdminNotificationRepository extends JpaRepository<AdminNotification, Long> {

    /**
     * Find all unread and not dismissed notifications ordered by creation date
     * descending
     */
    List<AdminNotification> findByReadFalseAndDismissedFalseOrderByCreatedAtDesc();

    /**
     * Find all notifications ordered by creation date descending
     */
    List<AdminNotification> findAllByOrderByCreatedAtDesc();

    /**
     * Count unread and not dismissed notifications
     */
    long countByReadFalseAndDismissedFalse();

    /**
     * Find all non-dismissed notifications (regardless of read status)
     */
    List<AdminNotification> findByDismissedFalseOrderByCreatedAtDesc();

    /**
     * Check if a notification exists for a given period and type to avoid
     * duplicates
     */
    boolean existsByNotificationTypeAndPeriodStartAndPeriodEnd(String notificationType, Date periodStart,
            Date periodEnd);

    /**
     * Find notifications created after a specific date
     */
    List<AdminNotification> findByCreatedAtAfterOrderByCreatedAtDesc(Date date);
}
