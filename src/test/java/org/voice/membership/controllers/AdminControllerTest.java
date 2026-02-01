package org.voice.membership.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.voice.membership.entities.Role;
import org.voice.membership.entities.User;
import org.voice.membership.repositories.UserRepository;

import java.util.Date;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Functional tests for AdminController
 * Tests admin dashboard and user management functionality
 * Uses real database with test data
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional // Rollback after each test
class AdminControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private UserRepository userRepository;

        private User adminUser;
        private User regularUser;

        @BeforeEach
        void setUp() {
                userRepository.deleteAll();

                // Create REAL test data in the database
                adminUser = User.builder()
                                .firstName("Admin")
                                .middleName(null)
                                .lastName("User")
                                .email("tarparakrimy1@gmail.com")
                                .password("Caspstone36!")
                                .role(Role.ADMIN.name())
                                .creation(new Date())
                                .build();
                adminUser = userRepository.save(adminUser);

                regularUser = User.builder()
                                .firstName("Regular")
                                .middleName(null)
                                .lastName("User")
                                .email("user@example.com")
                                .password("Capstone36!")
                                .phone("1234567890")
                                .address("123 Test St")
                                .postalCode("12345")
                                .role(Role.USER.name())
                                .creation(new Date())
                                .build();
                regularUser = userRepository.save(regularUser);
        }

        // ========================== Positive Test Cases ==========================
        // Admin Access Test
        @Test
        @WithMockUser(username = "tarparakrimy1@gmail.com", roles = "ADMIN")
        void adminDashboard_WithAdminRole_ShouldReturnDashboard() throws Exception {
                mockMvc.perform(get("/admin/dashboard"))
                                .andExpect(status().isOk())
                                .andExpect(view().name("admin"))
                                .andExpect(model().attributeExists("users"))
                                .andExpect(model().attributeExists("totalUsers"));
        }

        // Get User Details - Valid ID
        @Test
        @WithMockUser(username = "tarparakrimy1@gmail.com", roles = "ADMIN")
        void getUserDetails_WithValidId_ShouldReturnUserDetails() throws Exception {
                mockMvc.perform(get("/admin/user/" + regularUser.getId()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(regularUser.getId()))
                                .andExpect(jsonPath("$.name").value("Regular User"))
                                .andExpect(jsonPath("$.email").value("user@example.com"));
        }

        // Address Filter
        @Test
        @WithMockUser(username = "tarparakrimy1@gmail.com", roles = "ADMIN")
        void adminDashboard_WithAddressFilter_ShouldFilterUsers() throws Exception {
                mockMvc.perform(get("/admin/dashboard")
                                .param("address", "123 Test St"))
                                .andExpect(status().isOk())
                                .andExpect(view().name("admin"))
                                .andExpect(model().attributeExists("users"));
        }

        // Export Users to Excel
        @Test
        @WithMockUser(username = "tarparakrimy1@gmail.com", roles = "ADMIN")
        void exportUsers_ShouldDownloadExcelFile() throws Exception {
                mockMvc.perform(get("/admin/export-users"))
                                .andExpect(status().isOk())
                                .andExpect(header().string("Content-Type",
                                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        }

        // ========================== Negative Test Cases ==========================
        // Unauthenticated Access
        @Test
        void adminDashboard_WithoutAuthentication_ShouldRedirectToLogin() throws Exception {
                mockMvc.perform(get("/admin/dashboard"))
                                .andExpect(status().is3xxRedirection());// redirect to login page
        }

        // Get User Details - Invalid ID
        @Test
        @WithMockUser(username = "tarparakrimy1@gmail.com", roles = "ADMIN")
        void getUserDetails_WithInvalidId_ShouldReturnNotFound() throws Exception {
                mockMvc.perform(get("/admin/user/999"))
                                .andExpect(status().isNotFound());
        }

        // Regular User Blocked
        @Test
        @WithMockUser(username = "user@example.com", roles = "USER")
        void adminDashboard_WithUserRole_ShouldBeForbidden() throws Exception {
                mockMvc.perform(get("/admin/dashboard"))
                                .andExpect(status().isForbidden());
        }
}
