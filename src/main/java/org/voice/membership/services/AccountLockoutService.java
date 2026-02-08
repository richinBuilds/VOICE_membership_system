package org.voice.membership.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.voice.membership.entities.User;
import org.voice.membership.repositories.UserRepository;

import java.util.Date;

/**
 * Service responsible for managing account lockout functionality.
 * Handles tracking failed login attempts, locking/unlocking accounts,
 * and checking lockout status.
 */
@Service
public class AccountLockoutService {

    @Autowired
    private UserRepository userRepository;

    @Value("${account.lockout.max-attempts:5}")
    private int maxFailedAttempts;

    @Value("${account.lockout.duration-minutes:30}")
    private int lockoutDurationMinutes;

    /**
     * Records a failed login attempt for the user.
     * If max attempts reached, locks the account.
     *
     * @param email User's email address
     */
    @Transactional
    public void recordFailedLoginAttempt(String email) {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            return; // User doesn't exist, no action needed
        }

        // Check if account is already locked
        if (user.isAccountLocked()) {
            return; // Already locked, no need to increment
        }

        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);

        if (attempts >= maxFailedAttempts) {
            lockAccount(user);
        }

        userRepository.save(user);
    }

    /**
     * Locks the user account and sets the lockout time.
     *
     * @param user User to lock
     */
    private void lockAccount(User user) {
        user.setAccountLocked(true);
        user.setLockoutTime(new Date());
    }

    /**
     * Resets failed login attempts after successful login.
     *
     * @param email User's email address
     */
    @Transactional
    public void resetFailedAttempts(String email) {
        User user = userRepository.findByEmail(email);
        if (user != null) {
            user.setFailedLoginAttempts(0);
            user.setAccountLocked(false);
            user.setLockoutTime(null);
            userRepository.save(user);
        }
    }

    /**
     * Checks if the account is currently locked and if the lockout period has
     * expired.
     * If expired, automatically unlocks the account.
     *
     * @param email User's email address
     * @return true if account is locked and lockout period hasn't expired
     */
    @Transactional
    public boolean isAccountLocked(String email) {
        User user = userRepository.findByEmail(email);
        if (user == null || !user.isAccountLocked()) {
            return false;
        }

        // Check if lockout period has expired
        Date lockoutTime = user.getLockoutTime();
        if (lockoutTime != null) {
            long lockoutDurationMillis = lockoutDurationMinutes * 60 * 1000L;
            long timeSinceLockout = System.currentTimeMillis() - lockoutTime.getTime();

            if (timeSinceLockout >= lockoutDurationMillis) {
                // Lockout period expired, unlock the account
                unlockAccount(user);
                return false;
            }
        }

        return true;
    }

    /**
     * Manually unlocks an account (admin function or after lockout period expires).
     *
     * @param user User to unlock
     */
    @Transactional
    public void unlockAccount(User user) {
        user.setAccountLocked(false);
        user.setFailedLoginAttempts(0);
        user.setLockoutTime(null);
        userRepository.save(user);
    }

    /**
     * Gets the remaining lockout time in minutes.
     *
     * @param email User's email address
     * @return remaining lockout time in minutes, or 0 if not locked
     */
    public long getRemainingLockoutTime(String email) {
        User user = userRepository.findByEmail(email);
        if (user == null || !user.isAccountLocked() || user.getLockoutTime() == null) {
            return 0;
        }

        long lockoutDurationMillis = lockoutDurationMinutes * 60 * 1000L;
        long timeSinceLockout = System.currentTimeMillis() - user.getLockoutTime().getTime();
        long remainingMillis = lockoutDurationMillis - timeSinceLockout;

        return Math.max(0, remainingMillis / (60 * 1000)); // Convert to minutes
    }

    /**
     * Gets the number of remaining login attempts before lockout.
     *
     * @param email User's email address
     * @return remaining attempts
     */
    public int getRemainingAttempts(String email) {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            return maxFailedAttempts;
        }
        return Math.max(0, maxFailedAttempts - user.getFailedLoginAttempts());
    }

    public int getMaxFailedAttempts() {
        return maxFailedAttempts;
    }

    public int getLockoutDurationMinutes() {
        return lockoutDurationMinutes;
    }
}
