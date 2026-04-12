package org.voice.membership.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.voice.membership.dtos.ApiResponse;
import org.voice.membership.dtos.NotificationDetailsResponse;
import org.voice.membership.dtos.NotificationDTO;
import org.voice.membership.dtos.UnreadCountResponse;
import org.voice.membership.services.AdminNotificationFacadeService;

import java.util.List;

/**
 * notifications.
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/notifications")
@RequiredArgsConstructor
public class AdminNotificationController {

    private final AdminNotificationFacadeService notificationFacadeService;

    /**
     * Get all unread notifications
     */
    @GetMapping("/unread")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<NotificationDTO>>> getUnreadNotifications() {
        return ResponseEntity.ok(ApiResponse.success("Unread notifications fetched", notificationFacadeService.getUnreadNotifications()));
    }

    /**
     * Get all notifications
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<NotificationDTO>>> getAllNotifications() {
        return ResponseEntity.ok(ApiResponse.success("Notifications fetched", notificationFacadeService.getAllNotifications()));
    }

    /**
     * Get unread notification count
     */
    @GetMapping("/count")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UnreadCountResponse>> getUnreadCount() {
        return ResponseEntity.ok(ApiResponse.success(
                "Unread count fetched",
                UnreadCountResponse.builder().count(notificationFacadeService.getUnreadCount()).build()));
    }

    /**
     * Mark a notification as read
     */
    @PutMapping("/{id}/read")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable Long id) {
        notificationFacadeService.markAsRead(id);
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read", null));
    }

    /**
     * Mark all notifications as read
     */
    @PutMapping("/read-all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead() {
        notificationFacadeService.markAllAsRead();
        return ResponseEntity.ok(ApiResponse.success("All notifications marked as read", null));
    }

    /**
     * Dismiss a notification
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> dismissNotification(@PathVariable Long id) {
        notificationFacadeService.dismissNotification(id);
        return ResponseEntity.ok(ApiResponse.success("Notification dismissed", null));
    }

    /**
     * Dismiss notification(s) associated with a specific user.
     */
    @DeleteMapping("/dismiss-by-user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> dismissNotificationByUser(@PathVariable Integer userId) {
        boolean dismissed = notificationFacadeService.dismissNotificationForUser(userId);
        if (!dismissed) {
            return ResponseEntity.status(404)
                    .body(ApiResponse.error("No active notification found for this user", null));
        }
        return ResponseEntity.ok(ApiResponse.success("Notification dismissed", null));
    }

    /**
     * Dismiss all notifications
     */
    @PostMapping("/dismiss-all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> dismissAllNotifications() {
        notificationFacadeService.dismissAllNotifications();
        return ResponseEntity.ok(ApiResponse.success("All notifications dismissed", null));
    }

    /**
     * Get notification details including the list of new members
     */
    @GetMapping("/{id}/details")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<NotificationDetailsResponse>> getNotificationDetails(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Notification details fetched", notificationFacadeService.getNotificationDetails(id)));
    }
}
