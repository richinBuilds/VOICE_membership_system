package org.voice.membership.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.voice.membership.entities.User;
import org.voice.membership.repositories.UserRepository;

import java.util.Date;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for account lockout functionality
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
        "account.lockout.max-attempts=3",
        "account.lockout.duration-minutes=5"
})
@DisplayName("Account Lockout Integration Tests")
class AccountLockoutIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private static final String TEST_EMAIL = "lockout-test@example.com";
    private static final String CORRECT_PASSWORD = "ValidPass123!";
    private static final String WRONG_PASSWORD = "WrongPassword";

    @BeforeEach
    void setUp() {
        // Clean up any existing test user
        User existingUser = userRepository.findByEmail(TEST_EMAIL);
        if (existingUser != null) {
            userRepository.delete(existingUser);
        }

        // Create test user
        testUser = User.builder()
                .email(TEST_EMAIL)
                .password(passwordEncoder.encode(CORRECT_PASSWORD))
                .firstName("Test")
                .lastName("User")
                .role("USER")
                .creation(new Date())
                .emailVerified(true)
                .failedLoginAttempts(0)
                .accountLocked(false)
                .build();

        userRepository.save(testUser);
    }

    @Test
    @DisplayName("Should lock account after 3 failed login attempts")
    void testAccountLockAfterMultipleFailedAttempts() throws Exception {
        // First failed attempt
        mockMvc.perform(post("/login")
                .param("username", TEST_EMAIL)
                .param("password", WRONG_PASSWORD)
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login?error=true&remaining=2"));

        // Second failed attempt
        mockMvc.perform(post("/login")
                .param("username", TEST_EMAIL)
                .param("password", WRONG_PASSWORD)
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login?error=true&remaining=1"));

        // Third failed attempt - should lock the account
        mockMvc.perform(post("/login")
                .param("username", TEST_EMAIL)
                .param("password", WRONG_PASSWORD)
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login?locked=true&minutes=*"));

        // Verify user is locked in database
        User lockedUser = userRepository.findByEmail(TEST_EMAIL);
        assert lockedUser != null;
        assert lockedUser.isAccountLocked();
        assert lockedUser.getFailedLoginAttempts() == 3;
        assert lockedUser.getLockoutTime() != null;
    }

    @Test
    @DisplayName("Should not allow login when account is locked")
    void testCannotLoginWhenAccountLocked() throws Exception {
        // Manually lock the account
        testUser.setAccountLocked(true);
        testUser.setFailedLoginAttempts(3);
        testUser.setLockoutTime(new Date());
        userRepository.save(testUser);

        // Try to login with correct password
        mockMvc.perform(post("/login")
                .param("username", TEST_EMAIL)
                .param("password", CORRECT_PASSWORD)
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login?locked=true&minutes=*"));
    }

    @Test
    @DisplayName("Should reset failed attempts after successful login")
    void testResetAttemptsAfterSuccessfulLogin() throws Exception {
        // Set some failed attempts
        testUser.setFailedLoginAttempts(2);
        userRepository.save(testUser);

        // Successful login
        mockMvc.perform(post("/login")
                .param("username", TEST_EMAIL)
                .param("password", CORRECT_PASSWORD)
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"));

        // Verify failed attempts were reset
        User updatedUser = userRepository.findByEmail(TEST_EMAIL);
        assert updatedUser != null;
        assert updatedUser.getFailedLoginAttempts() == 0;
        assert !updatedUser.isAccountLocked();
    }

    @Test
    @DisplayName("Should show remaining attempts in error message")
    void testShowRemainingAttempts() throws Exception {
        // First failed attempt
        mockMvc.perform(post("/login")
                .param("username", TEST_EMAIL)
                .param("password", WRONG_PASSWORD)
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error=true&remaining=2"));
    }

    @Test
    @DisplayName("Should handle non-existent user login attempt gracefully")
    void testNonExistentUserLogin() throws Exception {
        mockMvc.perform(post("/login")
                .param("username", "nonexistent@example.com")
                .param("password", WRONG_PASSWORD)
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login?error=true*"));
    }

    @Test
    @DisplayName("Should not increment attempts when account already locked")
    void testNoIncrementWhenAlreadyLocked() throws Exception {
        // Lock the account
        testUser.setAccountLocked(true);
        testUser.setFailedLoginAttempts(3);
        testUser.setLockoutTime(new Date());
        userRepository.save(testUser);

        // Try another failed login
        mockMvc.perform(post("/login")
                .param("username", TEST_EMAIL)
                .param("password", WRONG_PASSWORD)
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login?locked=true&minutes=*"));

        // Verify attempts didn't increase
        User user = userRepository.findByEmail(TEST_EMAIL);
        assert user != null;
        assert user.getFailedLoginAttempts() == 3;
    }
}
