 package org.voice.membership.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.voice.membership.entities.Membership;
import org.voice.membership.entities.User;
import org.voice.membership.repositories.UserRepository;

import java.lang.reflect.Field;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MembershipRenewalSchedulerService.
 *
 * Verifies that:
 * - Reminder emails are sent to eligible paid members at the 30/14/7-day windows.
 * - No emails are sent when no members are expiring.
 * - A per-member email failure does not abort the whole batch.
 */
@ExtendWith(MockitoExtension.class)
class MembershipRenewalSchedulerServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailSenderService emailSenderService;

    @InjectMocks
    private MembershipRenewalSchedulerService schedulerService;

    private User paidUser;
    private Membership premiumMembership;

    @BeforeEach
    void setUp() throws Exception {
        // Inject the baseUrl field via reflection (normally bound from application.yaml)
        Field baseUrlField = MembershipRenewalSchedulerService.class.getDeclaredField("baseUrl");
        baseUrlField.setAccessible(true);
        baseUrlField.set(schedulerService, "http://localhost:8080");

        premiumMembership = new Membership();
        premiumMembership.setId(1);
        premiumMembership.setName("Premium Membership");
        premiumMembership.setFree(false);

        // Build a paid user expiring in 7 days from now
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, 7);

        paidUser = User.builder()
                .id(1)
                .firstName("Alice")
                .lastName("Smith")
                .email("alice@example.com")
                .paid(true)
                .membership(premiumMembership)
                .membershipExpiryDate(cal.getTime())
                .build();
    }

    // ==================== sendRenewalReminders Tests ====================

    @Test
    void sendRenewalReminders_WhenMemberExpiresIn7Days_ShouldSendEmail() {
        // Arrange: scheduler checks 30, 14, then 7-day windows – return match only on the third call
        doReturn(Collections.emptyList())
                .doReturn(Collections.emptyList())
                .doReturn(List.of(paidUser))
                .when(userRepository).findPaidMembersExpiringBetween(any(Date.class), any(Date.class));

        // Act
        Map<String, Object> result = schedulerService.sendRenewalReminders();

        // Assert: email sent exactly once (for the 7-day slot)
        verify(emailSenderService, times(1)).sendRenewalReminderEmail(
                eq("alice@example.com"),
                eq("Alice"),
                eq("Premium Membership"),
                anyString(),
                anyLong(),
                eq("http://localhost:8080/upgrade-membership")
        );
        assertThat(result.get("totalEmailsSent")).isEqualTo(1);
        assertThat(result.get("totalMembersFound")).isEqualTo(1);
    }

    @Test
    void sendRenewalReminders_WhenNoExpiringMembers_ShouldSendNoEmails() {
        // Arrange: no members expiring in any window
        when(userRepository.findPaidMembersExpiringBetween(any(Date.class), any(Date.class)))
                .thenReturn(Collections.emptyList());

        // Act
        Map<String, Object> result = schedulerService.sendRenewalReminders();

        // Assert
        verify(emailSenderService, never()).sendRenewalReminderEmail(
                anyString(), anyString(), anyString(), anyString(), anyLong(), anyString());
        assertThat(result.get("totalEmailsSent")).isEqualTo(0);
        assertThat(result.get("totalMembersFound")).isEqualTo(0);
    }

    @Test
    void sendRenewalReminders_WhenMultipleMembersExpiring_ShouldSendEmailToEach() {
        // Arrange: two members expiring in 30-day window
        User secondUser = User.builder()
                .id(2)
                .firstName("Bob")
                .lastName("Jones")
                .email("bob@example.com")
                .paid(true)
                .membership(premiumMembership)
                .membershipExpiryDate(paidUser.getMembershipExpiryDate())
                .build();

        when(userRepository.findPaidMembersExpiringBetween(any(Date.class), any(Date.class)))
                .thenReturn(List.of(paidUser, secondUser))  // 30-day window
                .thenReturn(Collections.emptyList())         // 14-day window
                .thenReturn(Collections.emptyList());        // 7-day window

        // Act
        Map<String, Object> result = schedulerService.sendRenewalReminders();

        // Assert: one email per member for the 30-day window
        verify(emailSenderService, times(1)).sendRenewalReminderEmail(
                eq("alice@example.com"), anyString(), anyString(), anyString(), anyLong(), anyString());
        verify(emailSenderService, times(1)).sendRenewalReminderEmail(
                eq("bob@example.com"), anyString(), anyString(), anyString(), anyLong(), anyString());
        assertThat(result.get("totalEmailsSent")).isEqualTo(2);
    }

    @Test
    void sendRenewalReminders_WhenEmailFailsForOneMember_ShouldContinueForOthers() {
        // Arrange: two members in the 7-day window; first throws on send
        User failUser = User.builder()
                .id(3)
                .firstName("Charlie")
                .lastName("Brown")
                .email("bad-email@example.com")
                .paid(true)
                .membership(premiumMembership)
                .membershipExpiryDate(paidUser.getMembershipExpiryDate())
                .build();

        when(userRepository.findPaidMembersExpiringBetween(any(Date.class), any(Date.class)))
                .thenReturn(Collections.emptyList())
                .thenReturn(Collections.emptyList())
                .thenReturn(List.of(failUser, paidUser));

        doThrow(new RuntimeException("SMTP connection refused"))
                .when(emailSenderService).sendRenewalReminderEmail(
                        eq("bad-email@example.com"), anyString(), anyString(), anyString(), anyLong(), anyString());

        // Act – must not throw
        Map<String, Object> result = schedulerService.sendRenewalReminders();

        // Assert: alice still received her email despite charlie's failure
        verify(emailSenderService, times(1)).sendRenewalReminderEmail(
                eq("alice@example.com"), anyString(), anyString(), anyString(), anyLong(), anyString());
        assertThat(result.get("totalEmailsSent")).isEqualTo(1);
        assertThat(result.get("totalEmailsFailed")).isEqualTo(1);
    }

    @Test
    void sendRenewalReminders_WhenMemberHasNoMembership_ShouldUseDefaultName() {
        // Arrange: user with null membership object
        User noMembershipUser = User.builder()
                .id(4)
                .firstName("Dana")
                .lastName("White")
                .email("dana@example.com")
                .paid(true)
                .membership(null)
                .membershipExpiryDate(paidUser.getMembershipExpiryDate())
                .build();

        when(userRepository.findPaidMembersExpiringBetween(any(Date.class), any(Date.class)))
                .thenReturn(List.of(noMembershipUser))
                .thenReturn(Collections.emptyList())
                .thenReturn(Collections.emptyList());

        // Act
        Map<String, Object> result = schedulerService.sendRenewalReminders();

        // Assert: falls back to "Premium Membership"
        verify(emailSenderService, times(1)).sendRenewalReminderEmail(
                eq("dana@example.com"),
                eq("Dana"),
                eq("Premium Membership"),
                anyString(),
                anyLong(),
                anyString()
        );
        assertThat(result.get("totalEmailsSent")).isEqualTo(1);
    }

    // ==================== previewExpiringMembers Tests ====================

    @Test
    void previewExpiringMembers_ShouldReturnMemberInfoWithoutSendingEmail() {
        // Arrange
        when(userRepository.findPaidMembersExpiringBetween(any(Date.class), any(Date.class)))
                .thenReturn(List.of(paidUser));

        // Act
        var preview = schedulerService.previewExpiringMembers(30);

        // Assert: member details returned, no email sent
        assertThat(preview).hasSize(1);
        assertThat(preview.get(0).get("email")).isEqualTo("alice@example.com");
        assertThat(preview.get(0).get("membership")).isEqualTo("Premium Membership");
        assertThat(preview.get(0).get("paid")).isEqualTo(true);
        verify(emailSenderService, never()).sendRenewalReminderEmail(
                anyString(), anyString(), anyString(), anyString(), anyLong(), anyString());
    }

    @Test
    void previewExpiringMembers_WhenNoMembers_ShouldReturnEmptyList() {
        when(userRepository.findPaidMembersExpiringBetween(any(Date.class), any(Date.class)))
                .thenReturn(Collections.emptyList());

        var preview = schedulerService.previewExpiringMembers(30);

        assertThat(preview).isEmpty();
        verify(emailSenderService, never()).sendRenewalReminderEmail(
                anyString(), anyString(), anyString(), anyString(), anyLong(), anyString());
    }
}
