package org.voice.membership.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.voice.membership.entities.Role;
import org.voice.membership.entities.User;
import org.voice.membership.repositories.UserRepository;
import org.voice.membership.services.UserService;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end workflow tests for Authentication
 * Tests complete user journeys: login workflows, password reset workflows
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthenticationWorkflowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        testUser = User.builder()
                .name("Auth Test User")
                .email("auth-test@example.com")
                .password(passwordEncoder.encode("TestPass123!"))
                .phone("1234567890")
                .role(Role.USER.name())
                .creation(new Date())
                .build();

        testUser = userRepository.save(testUser);
    }

    // Test Login Page Loads
    @Test
    void testLoginPageAccessible() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    // Test Complete Password Reset
    @Test
    void testCompletePasswordResetWorkflow() throws Exception {
        // Step 1: Request password reset
        mockMvc.perform(get("/forgot-password"))
                .andExpect(status().isOk())
                .andExpect(view().name("forgot-password"));

        // Step 2: Submit email for reset
        mockMvc.perform(post("/forgot-password")
                .with(csrf())
                .param("email", "auth-test@example.com"))
                .andExpect(status().isOk())
                .andExpect(view().name("forgot-password"))
                .andExpect(model().attributeExists("message"));

        boolean emailSent = userService.sendPasswordResetEmail("auth-test@example.com");
        assertThat(emailSent).isTrue();
    }

    // Test Invalid Email for Password Reset
    @Test
    void testPasswordResetWithInvalidEmail() throws Exception {
        mockMvc.perform(post("/forgot-password")
                .with(csrf())
                .param("email", "nonexistent@example.com"))
                .andExpect(status().isOk())
                .andExpect(view().name("forgot-password"))
                .andExpect(model().attribute("message", "No account found with that email address."));
    }

    // Test Reset Password Page with Token
    @Test
    void testResetPasswordPageWithToken() throws Exception {
        // Note: We don't actually send email here to avoid requiring mail server
        // Just test that the reset password page loads with a token parameter
        mockMvc.perform(get("/reset-password")
                .param("token", "test-token"))
                .andExpect(status().isOk())
                .andExpect(view().name("reset-password"))
                .andExpect(model().attribute("token", "test-token"));
    }

    // Test Logout Functionality
    @Test
    void testLogoutFunctionality() throws Exception {
        mockMvc.perform(post("/logout")
                .with(csrf()))
                .andExpect(status().is3xxRedirection());
    }

    // Test Loading User by Username
    @Test
    void testUserLoadByUsername() {
        // Test UserDetailsService
        var userDetails = userService.loadUserByUsername("auth-test@example.com");

        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("auth-test@example.com");
        assertThat(userDetails.getAuthorities()).hasSize(1);
    }
}
