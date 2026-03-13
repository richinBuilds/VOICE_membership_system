package org.voice.membership.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.voice.membership.dtos.NotificationDTO;
import org.voice.membership.entities.AdminNotification;
import org.voice.membership.entities.User;
import org.voice.membership.services.AdminNotificationService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST Controller for managing admin notifications.
 * Provides endpoints for fetching, marking as read, and dismissing
 * notifications.
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/notifications")
@RequiredArgsConstructor
public class AdminNotificationController {

    private final AdminNotificationService notificationService;

    /**
     * Get all unread notifications
     */
    @GetMapping("/unread")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<NotificationDTO>> getUnreadNotifications() {
        List<AdminNotification> notifications = notificationService.getUnreadNotifications();
        List<NotificationDTO> dtos = notifications.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * Get all notifications
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<NotificationDTO>> getAllNotifications() {
        List<AdminNotification> notifications = notificationService.getAllNotifications();
        List<NotificationDTO> dtos = notifications.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * Get unread notification count
     */
    @GetMapping("/count")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Long>> getUnreadCount() {
        long count = notificationService.getUnreadCount();
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * Mark a notification as read
     */
    @PutMapping("/{id}/read")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok(Map.of("message", "Notification marked as read"));
    }

    /**
     * Mark all notifications as read
     */
    @PutMapping("/read-all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> markAllAsRead() {
        notificationService.markAllAsRead();
        return ResponseEntity.ok(Map.of("message", "All notifications marked as read"));
    }

    /**
     * Dismiss a notification
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> dismissNotification(@PathVariable Long id) {
        notificationService.dismissNotification(id);
        return ResponseEntity.ok(Map.of("message", "Notification dismissed"));
    }

    /**
     * Dismiss all notifications
     */
    @DeleteMapping("/dismiss-all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> dismissAllNotifications() {
        notificationService.dismissAllNotifications();
        return ResponseEntity.ok(Map.of("message", "All notifications dismissed"));
    }

    /**
     * Get notification details including the list of new members
     */
    @GetMapping("/{id}/details")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getNotificationDetails(@PathVariable Long id) {
        AdminNotification notification = notificationService.getNotificationById(id);
        if (notification == null) {
            return ResponseEntity.notFound().build();
        }

        List<User> newMembers = notificationService.getNewPaidMembersForNotification(id);

        Map<String, Object> response = new HashMap<>();
        response.put("notification", convertToDTO(notification));
        response.put("members", newMembers.stream().map(this::convertUserToSimpleDTO).collect(Collectors.toList()));

        // Mark as read when details are viewed
        notificationService.markAsRead(id);

        return ResponseEntity.ok(response);
    }

    /**
     * Convert AdminNotification entity to DTO
     */
    private NotificationDTO convertToDTO(AdminNotification notification) {
        return NotificationDTO.builder()
                .id(notification.getId())
                .message(notification.getMessage())
                .notificationType(notification.getNotificationType())
                .newMembersCount(notification.getNewMembersCount())
                .periodStart(notification.getPeriodStart())
                .periodEnd(notification.getPeriodEnd())
                .read(notification.isRead())
                .dismissed(notification.isDismissed())
                .createdAt(notification.getCreatedAt())
                .build();
    }

    /**
     * Convert User to a simple DTO with essential info
     */
    private Map<String, Object> convertUserToSimpleDTO(User user) {
        Map<String, Object> userDTO = new HashMap<>();
        userDTO.put("id", user.getId());
        userDTO.put("firstName", user.getFirstName());
        userDTO.put("lastName", user.getLastName());
        userDTO.put("email", user.getEmail());
        userDTO.put("membershipName", user.getMembership() != null ? user.getMembership().getName() : "N/A");
        userDTO.put("registrationDate", user.getCreation());
        return userDTO;
    }
}
