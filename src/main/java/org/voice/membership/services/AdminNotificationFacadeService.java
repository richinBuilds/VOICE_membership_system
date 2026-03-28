package org.voice.membership.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.voice.membership.dtos.NotificationDTO;
import org.voice.membership.dtos.NotificationDetailsResponse;
import org.voice.membership.dtos.SimpleUserResponse;
import org.voice.membership.entities.AdminNotification;
import org.voice.membership.entities.User;
import org.voice.membership.exceptions.ResourceNotFoundException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminNotificationFacadeService {

    private final AdminNotificationService notificationService;

    public List<NotificationDTO> getUnreadNotifications() {
        return notificationService.getUnreadNotifications().stream().map(this::toNotificationDto).toList();
    }

    public List<NotificationDTO> getAllNotifications() {
        return notificationService.getAllNotifications().stream().map(this::toNotificationDto).toList();
    }

    public long getUnreadCount() {
        return notificationService.getUnreadCount();
    }

    public void markAsRead(Long id) {
        notificationService.markAsRead(id);
    }

    public void markAllAsRead() {
        notificationService.markAllAsRead();
    }

    public void dismissNotification(Long id) {
        notificationService.dismissNotification(id);
    }

    public void dismissAllNotifications() {
        notificationService.dismissAllNotifications();
    }

    public NotificationDetailsResponse getNotificationDetails(Long id) {
        AdminNotification notification = notificationService.getNotificationById(id);
        if (notification == null) {
            throw new ResourceNotFoundException("Notification not found");
        }

        List<SimpleUserResponse> members = notificationService.getNewPaidMembersForNotification(id).stream()
                .map(this::toSimpleUserResponse)
                .toList();

        notificationService.markAsRead(id);

        return NotificationDetailsResponse.builder()
                .notification(toNotificationDto(notification))
                .members(members)
                .build();
    }

    private NotificationDTO toNotificationDto(AdminNotification notification) {
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

    private SimpleUserResponse toSimpleUserResponse(User user) {
        return SimpleUserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .membershipName(user.getMembership() != null ? user.getMembership().getName() : "N/A")
                .registrationDate(user.getCreation())
                .build();
    }
}
