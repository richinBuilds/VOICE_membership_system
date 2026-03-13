package org.voice.membership.dtos;

import lombok.*;

import java.util.Date;

/**
 * Data Transfer Object for AdminNotification.
 * Used to transfer notification data between backend and frontend.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDTO {
    private Long id;
    private String message;
    private String notificationType;
    private int newMembersCount;
    private Date periodStart;
    private Date periodEnd;
    private boolean read;
    private boolean dismissed;
    private Date createdAt;
}
