package org.voice.membership.services;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.voice.membership.entities.AdminNotification;
import org.voice.membership.entities.Membership;
import org.voice.membership.entities.Role;
import org.voice.membership.entities.User;
import org.voice.membership.repositories.AdminNotificationRepository;
import org.voice.membership.repositories.UserRepository;

import java.lang.reflect.Field;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AdminNotificationService
 * Tests notification creation for new members (instant, daily, weekly)
 */
@ExtendWith(MockitoExtension.class)
class AdminNotificationServiceTest {

    @Mock
    private AdminNotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private AdminNotificationService adminNotificationService;

    private User paidUser;
    private User freeUser;
    private Membership paidMembership;
    private Membership freeMembership;

    @BeforeEach
    void setUp() throws Exception {
        // Inject EntityManager mock using reflection
        Field entityManagerField = AdminNotificationService.class.getDeclaredField("entityManager");
        entityManagerField.setAccessible(true);
        entityManagerField.set(adminNotificationService, entityManager);

        paidMembership = new Membership();
        paidMembership.setId(1);
        paidMembership.setName("Premium");
        paidMembership.setFree(false);

        freeMembership = new Membership();
        freeMembership.setId(2);
        freeMembership.setName("Free");
        freeMembership.setFree(true);

        paidUser = User.builder()
                .id(1)
                .firstName("John")
                .middleName(null)
                .lastName("Doe")
                .email("john@example.com")
                .password("password")
                .role(Role.USER.name())
                .membership(paidMembership)
                .paid(true)
                .creation(new Date())
                .build();

        freeUser = User.builder()
                .id(2)
                .firstName("Jane")
                .middleName(null)
                .lastName("Smith")
                .email("jane@example.com")
                .password("password")
                .role(Role.USER.name())
                .membership(freeMembership)
                .paid(false)
                .creation(new Date())
                .build();
    }

    // ==================== Create Instant Notification Tests ====================

    @Test
    void createInstantNotification_ForPaidMember_ShouldCreateNotification() {
        // Act
        adminNotificationService.createInstantNotification(paidUser);

        // Assert
        verify(notificationRepository).save(argThat(notification ->
                notification.getMessage().contains("New paid member: John Doe") &&
                        notification.getMessage().contains("Premium membership") &&
                        notification.getNotificationType().equals("INSTANT") &&
                        notification.getNewMembersCount() == 1 &&
                        !notification.isRead() &&
                        !notification.isDismissed()
        ));
    }

    @Test
    void createInstantNotification_ForFreeMember_ShouldCreateNotification() {
        // Act
        adminNotificationService.createInstantNotification(freeUser);

        // Assert
        verify(notificationRepository).save(argThat(notification ->
                notification.getMessage().contains("New free member: Jane Smith") &&
                        notification.getMessage().contains("Free membership") &&
                        notification.getNotificationType().equals("INSTANT") &&
                        notification.getNewMembersCount() == 1
        ));
    }

    @Test
    void createInstantNotification_WithNullUser_ShouldNotCreateNotification() {
        // Act
        adminNotificationService.createInstantNotification(null);

        // Assert
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void createInstantNotification_WithUserWithoutMembership_ShouldCreateNotification() {
        // Arrange
        User userWithoutMembership = User.builder()
                .id(3)
                .firstName("Test")
                .middleName(null)
                .lastName("User")
                .email("test@example.com")
                .password("password")
                .role(Role.USER.name())
                .membership(null)
                .paid(false)
                .creation(new Date())
                .build();

        // Act
        adminNotificationService.createInstantNotification(userWithoutMembership);

        // Assert
        verify(notificationRepository).save(argThat(notification ->
                notification.getMessage().contains("Test User") &&
                        notification.getMessage().contains("No membership")
        ));
    }

    // ==================== Generate Notification Tests ====================

    @Test
    void generateNotification_WithNewMembers_ShouldCreateNotification() {
        // Arrange
        Date periodStart = new Date();
        Date periodEnd = new Date();
        List<User> newMembers = Arrays.asList(paidUser);

        when(notificationRepository.existsByNotificationTypeAndPeriodStartAndPeriodEnd(
                anyString(), any(Date.class), any(Date.class))).thenReturn(false);
        when(userRepository.findNewPaidMembersBetweenDates(any(Date.class), any(Date.class)))
                .thenReturn(newMembers);
        when(notificationRepository.save(any(AdminNotification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        AdminNotification result = adminNotificationService.generateNotification("DAILY", periodStart, periodEnd);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getNotificationType()).isEqualTo("DAILY");
        assertThat(result.getNewMembersCount()).isEqualTo(1);
        assertThat(result.getMessage()).contains("1 new paid member registered");
        verify(notificationRepository).save(any(AdminNotification.class));
    }

    @Test
    void generateNotification_WithMultipleNewMembers_ShouldCreateNotification() {
        // Arrange
        Date periodStart = new Date();
        Date periodEnd = new Date();
        User anotherUser = User.builder()
                .id(3)
                .firstName("Bob")
                .middleName(null)
                .lastName("Johnson")
                .email("bob@example.com")
                .membership(paidMembership)
                .paid(true)
                .build();
        List<User> newMembers = Arrays.asList(paidUser, anotherUser);

        when(notificationRepository.existsByNotificationTypeAndPeriodStartAndPeriodEnd(
                anyString(), any(Date.class), any(Date.class))).thenReturn(false);
        when(userRepository.findNewPaidMembersBetweenDates(any(Date.class), any(Date.class)))
                .thenReturn(newMembers);
        when(notificationRepository.save(any(AdminNotification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        AdminNotification result = adminNotificationService.generateNotification("WEEKLY", periodStart, periodEnd);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getNewMembersCount()).isEqualTo(2);
        assertThat(result.getMessage()).contains("2 new paid members registered");
    }

    @Test
    void generateNotification_WithNoNewMembers_ShouldCreateNotificationWithZeroCount() {
        // Arrange
        Date periodStart = new Date();
        Date periodEnd = new Date();
        List<User> newMembers = Collections.emptyList();

        when(notificationRepository.existsByNotificationTypeAndPeriodStartAndPeriodEnd(
                anyString(), any(Date.class), any(Date.class))).thenReturn(false);
        when(userRepository.findNewPaidMembersBetweenDates(any(Date.class), any(Date.class)))
                .thenReturn(newMembers);
        when(notificationRepository.save(any(AdminNotification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        AdminNotification result = adminNotificationService.generateNotification("DAILY", periodStart, periodEnd);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getNewMembersCount()).isEqualTo(0);
        assertThat(result.getMessage()).contains("No new paid members");
    }

    @Test
    void generateNotification_WhenNotificationAlreadyExists_ShouldReturnNull() {
        // Arrange
        Date periodStart = new Date();
        Date periodEnd = new Date();

        when(notificationRepository.existsByNotificationTypeAndPeriodStartAndPeriodEnd(
                "DAILY", periodStart, periodEnd)).thenReturn(true);

        // Act
        AdminNotification result = adminNotificationService.generateNotification("DAILY", periodStart, periodEnd);

        // Assert
        assertThat(result).isNull();
        verify(notificationRepository, never()).save(any());
    }

    // ==================== Get Notifications Tests ====================

    @Test
    void getUnreadNotifications_ShouldReturnUnreadNotifications() {
        // Arrange
        AdminNotification notification1 = new AdminNotification();
        notification1.setId(1L);
        notification1.setRead(false);
        notification1.setDismissed(false);

        AdminNotification notification2 = new AdminNotification();
        notification2.setId(2L);
        notification2.setRead(false);
        notification2.setDismissed(false);

        List<AdminNotification> unreadNotifications = Arrays.asList(notification1, notification2);
        when(notificationRepository.findByReadFalseAndDismissedFalseOrderByCreatedAtDesc())
                .thenReturn(unreadNotifications);

        // Act
        List<AdminNotification> result = adminNotificationService.getUnreadNotifications();

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(notification1, notification2);
    }

    @Test
    void getUnreadCount_ShouldReturnCorrectCount() {
        // Arrange
        when(notificationRepository.countByReadFalseAndDismissedFalse()).thenReturn(5L);

        // Act
        long count = adminNotificationService.getUnreadCount();

        // Assert
        assertThat(count).isEqualTo(5L);
    }

    @Test
    void getAllNotifications_ShouldReturnAllNotifications() {
        // Arrange
        AdminNotification notification1 = new AdminNotification();
        AdminNotification notification2 = new AdminNotification();
        AdminNotification notification3 = new AdminNotification();

        List<AdminNotification> allNotifications = Arrays.asList(notification1, notification2, notification3);
        when(notificationRepository.findAllByOrderByCreatedAtDesc()).thenReturn(allNotifications);

        // Act
        List<AdminNotification> result = adminNotificationService.getAllNotifications();

        // Assert
        assertThat(result).hasSize(3);
        assertThat(result).containsExactly(notification1, notification2, notification3);
    }

    // ==================== Mark As Read Tests ====================

    @Test
    void markAsRead_WithValidId_ShouldMarkNotificationAsRead() {
        // Arrange
        Long notificationId = 1L;
        AdminNotification notification = new AdminNotification();
        notification.setId(notificationId);
        notification.setRead(false);

        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));

        // Act
        adminNotificationService.markAsRead(notificationId);

        // Assert
        assertThat(notification.isRead()).isTrue();
        verify(notificationRepository).save(notification);
    }

    @Test
    void markAsRead_WithInvalidId_ShouldNotThrowException() {
        // Arrange
        Long notificationId = 999L;
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.empty());

        // Act & Assert - should not throw exception
        adminNotificationService.markAsRead(notificationId);
        verify(notificationRepository, never()).save(any());
    }

    // ==================== Dismiss Notification Tests ====================

    @Test
    void dismissNotification_WithValidId_ShouldDismissNotification() {
        // Arrange
        Long notificationId = 1L;
        AdminNotification notification = new AdminNotification();
        notification.setId(notificationId);
        notification.setDismissed(false);

        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));

        // Act
        adminNotificationService.dismissNotification(notificationId);

        // Assert
        assertThat(notification.isDismissed()).isTrue();
        verify(notificationRepository).save(notification);
    }

    @Test
    void dismissAllNotifications_ShouldDismissAllUnreadNotifications() {
        // Arrange
        AdminNotification notification1 = new AdminNotification();
        notification1.setId(1L);
        notification1.setDismissed(false);

        AdminNotification notification2 = new AdminNotification();
        notification2.setId(2L);
        notification2.setDismissed(false);

        List<AdminNotification> unreadNotifications = Arrays.asList(notification1, notification2);
        when(notificationRepository.findByDismissedFalseOrderByCreatedAtDesc()).thenReturn(unreadNotifications);

        // Act
        adminNotificationService.dismissAllNotifications();

        // Assert
        assertThat(notification1.isDismissed()).isTrue();
        assertThat(notification2.isDismissed()).isTrue();
        verify(notificationRepository).saveAll(unreadNotifications);
    }

        @Test
        void dismissNotificationForUser_WithMatchingInstantNotification_ShouldDismiss() {
                // Arrange
                Date now = new Date();
                paidUser.setCreation(now);

                AdminNotification instant = new AdminNotification();
                instant.setId(11L);
                instant.setNotificationType("INSTANT");
                instant.setDismissed(false);
                instant.setMessage("New paid member: John Doe joined with Premium membership");

                when(userRepository.findById(1)).thenReturn(Optional.of(paidUser));
                when(notificationRepository
                                .findByNotificationTypeAndDismissedFalseAndPeriodStartLessThanEqualAndPeriodEndGreaterThanEqualOrderByCreatedAtDesc(
                                                eq("INSTANT"), eq(now), eq(now)))
                                .thenReturn(List.of(instant));

                // Act
                boolean dismissed = adminNotificationService.dismissNotificationForUser(1);

                // Assert
                assertThat(dismissed).isTrue();
                assertThat(instant.isDismissed()).isTrue();
                verify(notificationRepository).saveAll(anyList());
        }

        @Test
        void dismissNotificationForUser_WithMissingUser_ShouldReturnFalse() {
                // Arrange
                when(userRepository.findById(999)).thenReturn(Optional.empty());

                // Act
                boolean dismissed = adminNotificationService.dismissNotificationForUser(999);

                // Assert
                assertThat(dismissed).isFalse();
                verify(notificationRepository, never())
                                .findByNotificationTypeAndDismissedFalseAndPeriodStartLessThanEqualAndPeriodEndGreaterThanEqualOrderByCreatedAtDesc(
                                                anyString(), any(Date.class), any(Date.class));
                verify(notificationRepository, never()).saveAll(anyList());
        }
}
