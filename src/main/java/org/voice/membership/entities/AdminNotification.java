package org.voice.membership.entities;

import jakarta.persistence.*;
import lombok.*;
import java.util.Date;

/**
 * Entity representing admin notifications for new paid member registrations.
 * Stores notification messages sent to admins on a daily or weekly basis.
 */
@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "admin_notifications")
public class AdminNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(name = "notification_type", nullable = false, length = 20)
    private String notificationType; // DAILY or WEEKLY

    @Column(name = "new_members_count", nullable = false)
    private int newMembersCount;

    @Column(name = "period_start", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date periodStart;

    @Column(name = "period_end", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date periodEnd;

    @Column(name = "is_read", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean read = false;

    @Column(name = "is_dismissed", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean dismissed = false;

    @Column(name = "created_at", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = new Date();
    }
}
