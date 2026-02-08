package org.voice.membership.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import org.voice.membership.entities.User;
import org.voice.membership.repositories.UserRepository;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AccountLockoutService
 */
@DisplayName("Account Lockout Service Tests")
class AccountLockoutServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AccountLockoutService accountLockoutService;

    private User testUser;
    private static final String TEST_EMAIL = "test@example.com";
    private static final int MAX_ATTEMPTS = 5;
    private static final int LOCKOUT_DURATION = 30;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Set configuration values
        ReflectionTestUtils.setField(accountLockoutService, "maxFailedAttempts", MAX_ATTEMPTS);
        ReflectionTestUtils.setField(accountLockoutService, "lockoutDurationMinutes", LOCKOUT_DURATION);

        // Initialize test user
        testUser = User.builder()
                .id(1)
                .email(TEST_EMAIL)
                .firstName("Test")
                .lastName("User")
                .failedLoginAttempts(0)
                .accountLocked(false)
                .lockoutTime(null)
                .build();
    }

    @Test
    @DisplayName("Should record failed login attempt and increment counter")
    void testRecordFailedLoginAttempt() {
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(testUser);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        accountLockoutService.recordFailedLoginAttempt(TEST_EMAIL);

        assertEquals(1, testUser.getFailedLoginAttempts());
        assertFalse(testUser.isAccountLocked());
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    @DisplayName("Should lock account after maximum failed attempts")
    void testLockAccountAfterMaxAttempts() {
        testUser.setFailedLoginAttempts(MAX_ATTEMPTS - 1);
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(testUser);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        accountLockoutService.recordFailedLoginAttempt(TEST_EMAIL);

        assertEquals(MAX_ATTEMPTS, testUser.getFailedLoginAttempts());
        assertTrue(testUser.isAccountLocked());
        assertNotNull(testUser.getLockoutTime());
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    @DisplayName("Should not increment attempts for non-existent user")
    void testRecordFailedLoginAttemptForNonExistentUser() {
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(null);

        accountLockoutService.recordFailedLoginAttempt(TEST_EMAIL);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should not increment attempts when account is already locked")
    void testDoNotIncrementWhenAlreadyLocked() {
        testUser.setAccountLocked(true);
        testUser.setFailedLoginAttempts(MAX_ATTEMPTS);
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(testUser);

        accountLockoutService.recordFailedLoginAttempt(TEST_EMAIL);

        assertEquals(MAX_ATTEMPTS, testUser.getFailedLoginAttempts());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should reset failed attempts on successful login")
    void testResetFailedAttempts() {
        testUser.setFailedLoginAttempts(3);
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(testUser);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        accountLockoutService.resetFailedAttempts(TEST_EMAIL);

        assertEquals(0, testUser.getFailedLoginAttempts());
        assertFalse(testUser.isAccountLocked());
        assertNull(testUser.getLockoutTime());
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    @DisplayName("Should return true when account is locked and lockout period not expired")
    void testIsAccountLockedWithinLockoutPeriod() {
        testUser.setAccountLocked(true);
        testUser.setLockoutTime(new Date()); // Just locked
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(testUser);

        boolean isLocked = accountLockoutService.isAccountLocked(TEST_EMAIL);

        assertTrue(isLocked);
    }

    @Test
    @DisplayName("Should unlock account when lockout period has expired")
    void testAutoUnlockAfterLockoutPeriod() {
        testUser.setAccountLocked(true);
        testUser.setFailedLoginAttempts(MAX_ATTEMPTS);
        // Set lockout time to 31 minutes ago (past the 30-minute lockout)
        long expiredTime = System.currentTimeMillis() - (31 * 60 * 1000);
        testUser.setLockoutTime(new Date(expiredTime));

        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(testUser);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        boolean isLocked = accountLockoutService.isAccountLocked(TEST_EMAIL);

        assertFalse(isLocked);
        assertFalse(testUser.isAccountLocked());
        assertEquals(0, testUser.getFailedLoginAttempts());
        assertNull(testUser.getLockoutTime());
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    @DisplayName("Should return false when account is not locked")
    void testIsAccountLockedReturnsFalseForUnlockedAccount() {
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(testUser);

        boolean isLocked = accountLockoutService.isAccountLocked(TEST_EMAIL);

        assertFalse(isLocked);
    }

    @Test
    @DisplayName("Should return false when user does not exist")
    void testIsAccountLockedForNonExistentUser() {
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(null);

        boolean isLocked = accountLockoutService.isAccountLocked(TEST_EMAIL);

        assertFalse(isLocked);
    }

    @Test
    @DisplayName("Should manually unlock account")
    void testManualUnlockAccount() {
        testUser.setAccountLocked(true);
        testUser.setFailedLoginAttempts(MAX_ATTEMPTS);
        testUser.setLockoutTime(new Date());
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        accountLockoutService.unlockAccount(testUser);

        assertFalse(testUser.isAccountLocked());
        assertEquals(0, testUser.getFailedLoginAttempts());
        assertNull(testUser.getLockoutTime());
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    @DisplayName("Should calculate remaining lockout time correctly")
    void testGetRemainingLockoutTime() {
        testUser.setAccountLocked(true);
        // Set lockout time to 10 minutes ago
        long tenMinutesAgo = System.currentTimeMillis() - (10 * 60 * 1000);
        testUser.setLockoutTime(new Date(tenMinutesAgo));
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(testUser);

        long remainingMinutes = accountLockoutService.getRemainingLockoutTime(TEST_EMAIL);

        // Should have approximately 20 minutes remaining (30 - 10)
        assertTrue(remainingMinutes >= 19 && remainingMinutes <= 21);
    }

    @Test
    @DisplayName("Should return zero remaining time for unlocked account")
    void testGetRemainingLockoutTimeForUnlockedAccount() {
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(testUser);

        long remainingMinutes = accountLockoutService.getRemainingLockoutTime(TEST_EMAIL);

        assertEquals(0, remainingMinutes);
    }

    @Test
    @DisplayName("Should return zero remaining time when lockout expired")
    void testGetRemainingLockoutTimeWhenExpired() {
        testUser.setAccountLocked(true);
        // Set lockout time to 40 minutes ago (past the 30-minute lockout)
        long expiredTime = System.currentTimeMillis() - (40 * 60 * 1000);
        testUser.setLockoutTime(new Date(expiredTime));
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(testUser);

        long remainingMinutes = accountLockoutService.getRemainingLockoutTime(TEST_EMAIL);

        assertEquals(0, remainingMinutes);
    }

    @Test
    @DisplayName("Should calculate remaining attempts correctly")
    void testGetRemainingAttempts() {
        testUser.setFailedLoginAttempts(2);
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(testUser);

        int remainingAttempts = accountLockoutService.getRemainingAttempts(TEST_EMAIL);

        assertEquals(3, remainingAttempts); // 5 max - 2 failed = 3 remaining
    }

    @Test
    @DisplayName("Should return max attempts for non-existent user")
    void testGetRemainingAttemptsForNonExistentUser() {
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(null);

        int remainingAttempts = accountLockoutService.getRemainingAttempts(TEST_EMAIL);

        assertEquals(MAX_ATTEMPTS, remainingAttempts);
    }

    @Test
    @DisplayName("Should return zero remaining attempts when at max")
    void testGetRemainingAttemptsWhenAtMax() {
        testUser.setFailedLoginAttempts(MAX_ATTEMPTS);
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(testUser);

        int remainingAttempts = accountLockoutService.getRemainingAttempts(TEST_EMAIL);

        assertEquals(0, remainingAttempts);
    }

    @Test
    @DisplayName("Should return configuration values correctly")
    void testGetConfigurationValues() {
        assertEquals(MAX_ATTEMPTS, accountLockoutService.getMaxFailedAttempts());
        assertEquals(LOCKOUT_DURATION, accountLockoutService.getLockoutDurationMinutes());
    }
}
