package org.voice.membership.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.voice.membership.entities.User;
import org.voice.membership.repositories.UserRepository;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Password Reset Controllers
 * Tests forgot password and reset password functionality using real services
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PasswordResetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        testUser = new User();
        testUser.setEmail("test@example.com");
        testUser.setPassword(passwordEncoder.encode("OldPassword123!"));
        testUser.setFirstName("Test");
        testUser.setMiddleName(null);
        testUser.setLastName("User");
        testUser.setRole("USER");
        testUser = userRepository.save(testUser);
    }

    // ======================Positive Tests======================
    // Show Forgot Password Page
    @Test
    void showForgotPasswordPage_ShouldReturnForgotPasswordPage() throws Exception {
        mockMvc.perform(get("/forgot-password"))
                .andExpect(status().isOk())
                .andExpect(view().name("forgot-password"));
    }

    // Submit Valid Email - Real test with existing user
    @Test
    void processForgotPassword_WithValidEmail_ShouldSendResetLink() throws Exception {
        mockMvc.perform(post("/forgot-password")
                .with(csrf())
                .param("email", "test@example.com"))
                .andExpect(status().isOk())
                .andExpect(view().name("forgot-password"))
                .andExpect(model().attributeExists("message"));
    }

    // Show Reset Password Page
    @Test
    void showResetPasswordPage_WithToken_ShouldReturnResetPasswordPage() throws Exception {
        mockMvc.perform(get("/reset-password")
                .param("token", "test-token"))
                .andExpect(status().isOk())
                .andExpect(view().name("reset-password"))
                .andExpect(model().attributeExists("token"));
    }

    // ======================Negative Tests======================
    // Submit Invalid Email - Real test with non-existent user
    @Test
    void processForgotPassword_WithInvalidEmail_ShouldReturnError() throws Exception {
        mockMvc.perform(post("/forgot-password")
                .with(csrf())
                .param("email", "nonexistent@example.com"))
                .andExpect(status().isOk())
                .andExpect(view().name("forgot-password"))
                .andExpect(model().attribute("message", "No account found with that email address."));
    }

    // Password Mismatch
    @Test
    void processResetPassword_WithPasswordMismatch_ShouldReturnError() throws Exception {
        mockMvc.perform(post("/reset-password")
                .with(csrf())
                .param("token", "test-token")
                .param("password", "NewPass123!")
                .param("confirmPassword", "DifferentPass123!"))
                .andExpect(status().isOk())
                .andExpect(view().name("reset-password"))
                .andExpect(model().attribute("message", "Passwords do not match."));
    }

    // Invalid Token
    @Test
    void processResetPassword_WithInvalidToken_ShouldReturnError() throws Exception {
        mockMvc.perform(post("/reset-password")
                .with(csrf())
                .param("token", "invalid-token")
                .param("password", "NewPass123!")
                .param("confirmPassword", "NewPass123!"))
                .andExpect(status().isOk())
                .andExpect(view().name("reset-password"))
                .andExpect(model().attribute("message", "Invalid or expired reset link."));
    }
}
