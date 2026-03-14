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
import org.springframework.http.MediaType;
import org.voice.membership.entities.Role;
import org.voice.membership.entities.User;
import org.voice.membership.repositories.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Functional tests for AdminController
 * Tests admin dashboard and user management functionality
 * Uses real database with test data
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private ObjectMapper objectMapper;

        private User adminUser;
        private User regularUser;

        @BeforeEach
        void setUp() {
                userRepository.deleteAll();

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
        @Test
        @WithMockUser(username = "tarparakrimy1@gmail.com", roles = "ADMIN")
        void adminDashboard_WithAdminRole_ShouldReturnDashboard() throws Exception {
                mockMvc.perform(get("/admin/dashboard"))
                                .andExpect(status().isOk())
                                .andExpect(view().name("admin"))
                                .andExpect(model().attributeExists("users"))
                                .andExpect(model().attributeExists("totalUsers"));
        }

        @Test
        @WithMockUser(username = "tarparakrimy1@gmail.com", roles = "ADMIN")
        void getUserDetails_WithValidId_ShouldReturnUserDetails() throws Exception {
                mockMvc.perform(get("/admin/user/" + regularUser.getId()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(regularUser.getId()))
                                .andExpect(jsonPath("$.firstName").value("Regular"))
                                .andExpect(jsonPath("$.lastName").value("User"))
                                .andExpect(jsonPath("$.email").value("user@example.com"));
        }

        @Test
        @WithMockUser(username = "tarparakrimy1@gmail.com", roles = "ADMIN")
        void adminDashboard_WithAddressFilter_ShouldFilterUsers() throws Exception {
                mockMvc.perform(get("/admin/dashboard")
                                .param("address", "123 Test St"))
                                .andExpect(status().isOk())
                                .andExpect(view().name("admin"))
                                .andExpect(model().attributeExists("users"));
        }

        @Test
        @WithMockUser(username = "tarparakrimy1@gmail.com", roles = "ADMIN")
        void exportUsers_ShouldDownloadExcelFile() throws Exception {
                mockMvc.perform(get("/admin/export-users"))
                                .andExpect(status().isOk())
                                .andExpect(header().string("Content-Type",
                                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        }

        // ========================== Negative Test Cases ==========================
        @Test
        void adminDashboard_WithoutAuthentication_ShouldRedirectToLogin() throws Exception {
                mockMvc.perform(get("/admin/dashboard"))
                                .andExpect(status().is3xxRedirection());// redirect to login page
        }

        @Test
        @WithMockUser(username = "tarparakrimy1@gmail.com", roles = "ADMIN")
        void getUserDetails_WithInvalidId_ShouldReturnNotFound() throws Exception {
                mockMvc.perform(get("/admin/user/999"))
                                .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(username = "user@example.com", roles = "USER")
        void adminDashboard_WithUserRole_ShouldBeForbidden() throws Exception {
                mockMvc.perform(get("/admin/dashboard"))
                                .andExpect(status().isForbidden());
        }

        // ========================== Edit Member Tests ==========================
        @Test
        @WithMockUser(username = "tarparakrimy1@gmail.com", roles = "ADMIN")
        void editMemberForm_WithValidId_ShouldShowForm() throws Exception {
                mockMvc.perform(get("/admin/edit-member/" + regularUser.getId()))
                                .andExpect(status().isOk())
                                .andExpect(view().name("admin-edit-member"))
                                .andExpect(model().attributeExists("updateRequest"))
                                .andExpect(model().attributeExists("user"))
                                .andExpect(model().attributeExists("memberships"));
        }

        @Test
        @WithMockUser(username = "tarparakrimy1@gmail.com", roles = "ADMIN")
        void editMemberForm_WithInvalidId_ShouldRedirectWithError() throws Exception {
                mockMvc.perform(get("/admin/edit-member/99999"))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/admin/dashboard"))
                                .andExpect(flash().attributeExists("error"));
        }

        @Test
        @WithMockUser(username = "tarparakrimy1@gmail.com", roles = "ADMIN")
        void updateMember_WithValidData_ShouldUpdateAndRedirect() throws Exception {
                mockMvc.perform(post("/admin/edit-member/" + regularUser.getId())
                                .with(csrf())
                                .param("userId", String.valueOf(regularUser.getId()))
                                .param("firstName", "UpdatedFirst")
                                .param("lastName", "UpdatedLast")
                                .param("email", "updated@example.com")
                                .param("phone", "9876543210")
                                .param("address", "456 New St")
                                .param("city", "Toronto")
                                .param("province", "ON")
                                .param("postalCode", "M1M1M1")
                                .param("emailVerified", "true")
                                .param("accountLocked", "false"))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/admin/dashboard"))
                                .andExpect(flash().attributeExists("success"));

                User updated = userRepository.findById(regularUser.getId()).orElse(null);
                assertThat(updated).isNotNull();
                assertThat(updated.getFirstName()).isEqualTo("UpdatedFirst");
                assertThat(updated.getLastName()).isEqualTo("UpdatedLast");
                assertThat(updated.getEmail()).isEqualTo("updated@example.com");
        }

        @Test
        @WithMockUser(username = "tarparakrimy1@gmail.com", roles = "ADMIN")
        void updateMember_WithDuplicateEmail_ShouldShowError() throws Exception {
                mockMvc.perform(post("/admin/edit-member/" + regularUser.getId())
                                .with(csrf())
                                .param("userId", String.valueOf(regularUser.getId()))
                                .param("firstName", "Regular")
                                .param("lastName", "User")
                                .param("email", adminUser.getEmail()) // Duplicate email
                                .param("phone", "1234567890")
                                .param("address", "123 Test St")
                                .param("city", "Test City")
                                .param("province", "TC")
                                .param("postalCode", "12345")
                                .param("emailVerified", "true")
                                .param("accountLocked", "false"))
                                .andExpect(status().isOk())
                                .andExpect(view().name("admin-edit-member"))
                                .andExpect(model().hasErrors());
        }

        @Test
        @WithMockUser(username = "tarparakrimy1@gmail.com", roles = "ADMIN")
        void updateMember_WithInvalidData_ShouldShowValidationErrors() throws Exception {
                mockMvc.perform(post("/admin/edit-member/" + regularUser.getId())
                                .with(csrf())
                                .param("userId", String.valueOf(regularUser.getId()))
                                .param("firstName", "") // Invalid: empty
                                .param("lastName", "") // Invalid: empty
                                .param("email", "invalid-email") // Invalid format
                                .param("phone", "123") // Invalid length
                                .param("emailVerified", "true")
                                .param("accountLocked", "false"))
                                .andExpect(status().isOk())
                                .andExpect(view().name("admin-edit-member"))
                                .andExpect(model().hasErrors());
        }

        // ========================== Add Member Tests ==========================
        @Test
        @WithMockUser(username = "tarparakrimy1@gmail.com", roles = "ADMIN")
        void addMemberForm_ShouldShowForm() throws Exception {
                mockMvc.perform(get("/admin/add-member"))
                                .andExpect(status().isOk())
                                .andExpect(view().name("admin-add-member"))
                                .andExpect(model().attributeExists("memberRequest"))
                                .andExpect(model().attributeExists("memberships"));
        }

        @Test
        @WithMockUser(username = "tarparakrimy1@gmail.com", roles = "ADMIN")
        void addMember_WithValidData_ShouldCreateAndRedirect() throws Exception {
                int initialCount = (int) userRepository.count();

                mockMvc.perform(post("/admin/add-member")
                                .with(csrf())
                                .param("firstName", "NewMember")
                                .param("lastName", "TestUser")
                                .param("email", "newmember@example.com")
                                .param("phone", "5551234567")
                                .param("address", "789 New Address")
                                .param("city", "Vancouver")
                                .param("province", "BC")
                                .param("postalCode", "V1V1V1")
                                .param("password", "Password123!")
                                .param("confirmPassword", "Password123!")
                                .param("emailVerified", "true")
                                .param("accountLocked", "false"))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/admin/dashboard"))
                                .andExpect(flash().attributeExists("success"));

                int finalCount = (int) userRepository.count();
                assertThat(finalCount).isEqualTo(initialCount + 1);

                User newMember = userRepository.findByEmail("newmember@example.com");
                assertThat(newMember).isNotNull();
                assertThat(newMember.getFirstName()).isEqualTo("NewMember");
                assertThat(newMember.getLastName()).isEqualTo("TestUser");
        }

        @Test
        @WithMockUser(username = "tarparakrimy1@gmail.com", roles = "ADMIN")
        void addMember_WithDuplicateEmail_ShouldShowError() throws Exception {
                mockMvc.perform(post("/admin/add-member")
                                .with(csrf())
                                .param("firstName", "Duplicate")
                                .param("lastName", "User")
                                .param("email", regularUser.getEmail()) // Existing email
                                .param("phone", "5551234567")
                                .param("address", "789 Test St")
                                .param("city", "Test City")
                                .param("province", "TC")
                                .param("postalCode", "12345")
                                .param("password", "Password123!")
                                .param("confirmPassword", "Password123!")
                                .param("emailVerified", "true")
                                .param("accountLocked", "false"))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/admin/add-member"))
                                .andExpect(flash().attributeExists("error"));
        }

        @Test
        @WithMockUser(username = "tarparakrimy1@gmail.com", roles = "ADMIN")
        void addMember_WithInvalidData_ShouldShowError() throws Exception {
                mockMvc.perform(post("/admin/add-member")
                                .with(csrf())
                                .param("firstName", "") // Invalid: empty
                                .param("lastName", "") // Invalid: empty
                                .param("email", "invalid-email") // Invalid format
                                .param("phone", "123") // Invalid length
                                .param("password", "weak")
                                .param("confirmPassword", "weak"))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/admin/add-member"))
                                .andExpect(flash().attributeExists("error"));
        }

        @Test
        @WithMockUser(username = "tarparakrimy1@gmail.com", roles = "ADMIN")
        void addMember_WithMismatchedPasswords_ShouldShowError() throws Exception {
                mockMvc.perform(post("/admin/add-member")
                                .with(csrf())
                                .param("firstName", "Test")
                                .param("lastName", "User")
                                .param("email", "test@example.com")
                                .param("phone", "5551234567")
                                .param("address", "123 Test St")
                                .param("city", "Test City")
                                .param("province", "TC")
                                .param("postalCode", "12345")
                                .param("password", "Password123!")
                                .param("confirmPassword", "DifferentPassword123!") // Mismatch
                                .param("emailVerified", "true")
                                .param("accountLocked", "false"))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/admin/add-member"))
                                .andExpect(flash().attributeExists("error"));
        }

        // ========================== Delete Member Tests ==========================
        @Test
        @WithMockUser(username = "tarparakrimy1@gmail.com", roles = "ADMIN")
        void deleteMember_WithValidId_ShouldDeleteAndRedirect() throws Exception {
                Integer userIdToDelete = regularUser.getId();

                mockMvc.perform(post("/admin/delete-member/" + userIdToDelete)
                                .with(csrf()))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/admin/dashboard"))
                                .andExpect(flash().attributeExists("success"));

                User deletedUser = userRepository.findById(userIdToDelete).orElse(null);
                assertThat(deletedUser).isNull();
        }

        @Test
        @WithMockUser(username = "tarparakrimy1@gmail.com", roles = "ADMIN")
        void deleteMember_WithInvalidId_ShouldShowError() throws Exception {
                mockMvc.perform(post("/admin/delete-member/99999")
                                .with(csrf()))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/admin/dashboard"))
                                .andExpect(flash().attributeExists("error"));
        }

        @Test
        @WithMockUser(username = "tarparakrimy1@gmail.com", roles = "ADMIN")
        void deleteMember_AdminAccount_ShouldShowError() throws Exception {
                mockMvc.perform(post("/admin/delete-member/" + adminUser.getId())
                                .with(csrf()))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/admin/dashboard"))
                                .andExpect(flash().attributeExists("error"))
                                .andExpect(flash().attribute("error", containsString("Cannot delete admin")));

                User admin = userRepository.findById(adminUser.getId()).orElse(null);
                assertThat(admin).isNotNull();
        }

        @Test
        @WithMockUser(username = "user@example.com", roles = "USER")
        void deleteMember_WithUserRole_ShouldBeForbidden() throws Exception {
                mockMvc.perform(post("/admin/delete-member/" + regularUser.getId())
                                .with(csrf()))
                                .andExpect(status().isForbidden());
        }

        // ========================== Create Additional Admin Account Tests
        // ==========================
        @Test
        @WithMockUser(username = "tarparakrimy1@gmail.com", roles = "ADMIN")
        void addAdminForm_ShouldShowForm() throws Exception {
                mockMvc.perform(get("/admin/add-admin"))
                                .andExpect(status().isOk())
                                .andExpect(view().name("admin-add-admin"))
                                .andExpect(model().attributeExists("adminRequest"));
        }

        @Test
        @WithMockUser(username = "tarparakrimy1@gmail.com", roles = "ADMIN")
        void addAdmin_WithValidData_ShouldCreateAdminAndRedirect() throws Exception {
                int initialCount = (int) userRepository.count();

                mockMvc.perform(post("/admin/add-admin")
                                .with(csrf())
                                .param("firstName", "NewAdmin")
                                .param("lastName", "TestAdmin")
                                .param("email", "newadmin@example.com")
                                .param("password", "AdminPass123!")
                                .param("confirmPassword", "AdminPass123!"))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/admin/dashboard?role=ADMIN"))
                                .andExpect(flash().attributeExists("success"));

                int finalCount = (int) userRepository.count();
                assertThat(finalCount).isEqualTo(initialCount + 1);

                User newAdmin = userRepository.findByEmail("newadmin@example.com");
                assertThat(newAdmin).isNotNull();
                assertThat(newAdmin.getFirstName()).isEqualTo("NewAdmin");
                assertThat(newAdmin.getLastName()).isEqualTo("TestAdmin");
                assertThat(newAdmin.getRole()).isEqualTo(Role.ADMIN.name());
        }

        @Test
        @WithMockUser(username = "tarparakrimy1@gmail.com", roles = "ADMIN")
        void addAdmin_WithDuplicateEmail_ShouldShowError() throws Exception {
                mockMvc.perform(post("/admin/add-admin")
                                .with(csrf())
                                .param("firstName", "Duplicate")
                                .param("lastName", "Admin")
                                .param("email", adminUser.getEmail()) // Existing email
                                .param("password", "Password123!")
                                .param("confirmPassword", "Password123!"))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/admin/add-admin"))
                                .andExpect(flash().attributeExists("error"));
        }

        @Test
        @WithMockUser(username = "tarparakrimy1@gmail.com", roles = "ADMIN")
        void addAdmin_WithMismatchedPasswords_ShouldShowError() throws Exception {
                mockMvc.perform(post("/admin/add-admin")
                                .with(csrf())
                                .param("firstName", "Test")
                                .param("lastName", "Admin")
                                .param("email", "testadmin@example.com")
                                .param("password", "Password123!")
                                .param("confirmPassword", "DifferentPassword123!")) // Mismatch
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/admin/add-admin"))
                                .andExpect(flash().attributeExists("error"));
        }

        @Test
        @WithMockUser(username = "user@example.com", roles = "USER")
        void addAdmin_WithUserRole_ShouldBeForbidden() throws Exception {
                mockMvc.perform(get("/admin/add-admin"))
                                .andExpect(status().isForbidden());
        }

        // ========================== Admin/User Tabs Separation Tests
        // ==========================
        @Test
        @WithMockUser(username = "tarparakrimy1@gmail.com", roles = "ADMIN")
        void adminDashboard_ShouldIncludeAdminsAndUsersData() throws Exception {
                // Create additional users for testing
                User anotherAdmin = User.builder()
                                .firstName("Second")
                                .middleName(null)
                                .lastName("Admin")
                                .email("admin2@example.com")
                                .password("Password123!")
                                .role(Role.ADMIN.name())
                                .creation(new Date())
                                .build();
                userRepository.save(anotherAdmin);

                User anotherUser = User.builder()
                                .firstName("Second")
                                .middleName(null)
                                .lastName("User")
                                .email("user2@example.com")
                                .password("Password123!")
                                .role(Role.USER.name())
                                .creation(new Date())
                                .build();
                userRepository.save(anotherUser);

                mockMvc.perform(get("/admin/dashboard"))
                                .andExpect(status().isOk())
                                .andExpect(view().name("admin"))
                                .andExpect(model().attributeExists("users"))
                                .andExpect(model().attributeExists("totalUsers"))
                                .andExpect(model().attributeExists("adminCount"))
                                .andExpect(model().attributeExists("userCount"));
        }

        @Test
        @WithMockUser(username = "tarparakrimy1@gmail.com", roles = "ADMIN")
        void adminDashboard_WithRoleFilter_ShouldFilterAdmins() throws Exception {
                // Create additional admin for testing
                User anotherAdmin = User.builder()
                                .firstName("Second")
                                .middleName(null)
                                .lastName("Admin")
                                .email("admin2@example.com")
                                .password("Password123!")
                                .role(Role.ADMIN.name())
                                .creation(new Date())
                                .build();
                userRepository.save(anotherAdmin);

                mockMvc.perform(get("/admin/dashboard")
                                .param("role", "ADMIN"))
                                .andExpect(status().isOk())
                                .andExpect(view().name("admin"))
                                .andExpect(model().attributeExists("users"));
        }

        @Test
        @WithMockUser(username = "tarparakrimy1@gmail.com", roles = "ADMIN")
        void adminDashboard_WithRoleFilter_ShouldFilterRegularUsers() throws Exception {
                mockMvc.perform(get("/admin/dashboard")
                                .param("role", "USER"))
                                .andExpect(status().isOk())
                                .andExpect(view().name("admin"))
                                .andExpect(model().attributeExists("users"));
        }

        // ========================== Chapter Filter Tests ==========================
        @Test
        @WithMockUser(username = "tarparakrimy1@gmail.com", roles = "ADMIN")
        void adminDashboard_WithChapterFilter_ShouldFilterByChapter() throws Exception {
                // Create users with different chapters
                User vancouverUser = User.builder()
                                .firstName("Vancouver")
                                .middleName(null)
                                .lastName("Member")
                                .email("vancouver@example.com")
                                .password("Password123!")
                                .role(Role.USER.name())
                                .chapter("Greater Vancouver")
                                .creation(new Date())
                                .build();
                userRepository.save(vancouverUser);

                User torontoUser = User.builder()
                                .firstName("Toronto")
                                .middleName(null)
                                .lastName("Member")
                                .email("toronto@example.com")
                                .password("Password123!")
                                .role(Role.USER.name())
                                .chapter("Greater Toronto")
                                .creation(new Date())
                                .build();
                userRepository.save(torontoUser);

                mockMvc.perform(get("/admin/dashboard")
                                .param("chapter", "Greater Vancouver"))
                                .andExpect(status().isOk())
                                .andExpect(view().name("admin"))
                                .andExpect(model().attributeExists("users"));
        }

        @Test
        @WithMockUser(username = "tarparakrimy1@gmail.com", roles = "ADMIN")
        void adminDashboard_WithMultipleFilters_ShouldApplyAllFilters() throws Exception {
                // Create user with specific attributes
                User specificUser = User.builder()
                                .firstName("Specific")
                                .middleName(null)
                                .lastName("User")
                                .email("specific@example.com")
                                .password("Password123!")
                                .role(Role.USER.name())
                                .chapter("Greater Vancouver")
                                .province("BC")
                                .city("Vancouver")
                                .creation(new Date())
                                .build();
                userRepository.save(specificUser);

                mockMvc.perform(get("/admin/dashboard")
                                .param("chapter", "Greater Vancouver")
                                .param("province", "BC")
                                .param("city", "Vancouver"))
                                .andExpect(status().isOk())
                                .andExpect(view().name("admin"))
                                .andExpect(model().attributeExists("users"));
        }

        // ========================== Admin Notification API Tests
        // ==========================
        @Test
        @WithMockUser(username = "tarparakrimy1@gmail.com", roles = "ADMIN")
        void getUnreadNotifications_ShouldReturnNotifications() throws Exception {
                mockMvc.perform(get("/admin/api/admin/notifications/unread"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isArray());
        }

        @Test
        @WithMockUser(username = "tarparakrimy1@gmail.com", roles = "ADMIN")
        void getUnreadNotificationsCount_ShouldReturnCount() throws Exception {
                mockMvc.perform(get("/admin/api/admin/notifications/count"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.count").isNumber());
        }

        @Test
        @WithMockUser(username = "tarparakrimy1@gmail.com", roles = "ADMIN")
        void dismissAllNotifications_ShouldSucceed() throws Exception {
                mockMvc.perform(post("/admin/api/admin/notifications/dismiss-all")
                                .with(csrf()))
                                .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(username = "user@example.com", roles = "USER")
        void getUnreadNotifications_WithUserRole_ShouldBeForbidden() throws Exception {
                mockMvc.perform(get("/admin/api/admin/notifications/unread"))
                                .andExpect(status().isForbidden());
        }

        @Test
        void getUnreadNotifications_WithoutAuth_ShouldRedirectToLogin() throws Exception {
                mockMvc.perform(get("/admin/api/admin/notifications/unread"))
                                .andExpect(status().is3xxRedirection());
        }

        // ========================== Notifications Page Test ==========================
        @Test
        @WithMockUser(username = "tarparakrimy1@gmail.com", roles = "ADMIN")
        void notificationsPage_ShouldRenderSuccessfully() throws Exception {
                mockMvc.perform(get("/admin/notifications"))
                                .andExpect(status().isOk())
                                .andExpect(view().name("admin-notifications"))
                                .andExpect(model().attributeExists("newUsers"));
        }

        @Test
        @WithMockUser(username = "user@example.com", roles = "USER")
        void notificationsPage_WithUserRole_ShouldBeForbidden() throws Exception {
                mockMvc.perform(get("/admin/notifications"))
                                .andExpect(status().isForbidden());
        }

        // ========================== Bulk Email Tests ==========================

        @Test
        @WithMockUser(username = "tarparakrimy1@gmail.com", roles = "ADMIN")
        void sendBulkEmail_WithValidRequest_ShouldSendSuccessfully() throws Exception {
                // Create additional test users
                final User user2 = userRepository.save(User.builder()
                                .firstName("Jane")
                                .lastName("Smith")
                                .email("jane@example.com")
                                .password("password")
                                .role(Role.USER.name())
                                .creation(new Date())
                                .build());

                final User user3 = userRepository.save(User.builder()
                                .firstName("Bob")
                                .lastName("Johnson")
                                .email("bob@example.com")
                                .password("password")
                                .role(Role.USER.name())
                                .creation(new Date())
                                .build());

                // Create bulk email request with multiple recipients
                String requestJson = objectMapper.writeValueAsString(new java.util.HashMap<String, Object>() {
                        {
                                put("recipientIds", Arrays.asList(regularUser.getId(), user2.getId(), user3.getId()));
                                put("subject", "Important Announcement");
                                put("messageBody", "This is a test bulk email message to all selected members.");
                        }
                });

                mockMvc.perform(post("/admin/send-bulk-email")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("success"))
                                .andExpect(jsonPath("$.successCount").value("3"))
                                .andExpect(jsonPath("$.failureCount").value("0"))
                                .andExpect(jsonPath("$.message")
                                                .value(containsString("Emails sent successfully to 3 recipient(s)")));
        }

        @Test
        @WithMockUser(username = "tarparakrimy1@gmail.com", roles = "ADMIN")
        void sendBulkEmail_WithNoRecipients_ShouldReturnBadRequest() throws Exception {
                String requestJson = objectMapper.writeValueAsString(new java.util.HashMap<String, Object>() {
                        {
                                put("recipientIds", Collections.emptyList());
                                put("subject", "Test Subject");
                                put("messageBody", "Test message body");
                        }
                });

                mockMvc.perform(post("/admin/send-bulk-email")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.status").value("error"))
                                .andExpect(jsonPath("$.message").value("No recipients selected"));
        }

        @Test
        @WithMockUser(username = "tarparakrimy1@gmail.com", roles = "ADMIN")
        void sendBulkEmail_WithEmptySubject_ShouldReturnBadRequest() throws Exception {
                String requestJson = objectMapper.writeValueAsString(new java.util.HashMap<String, Object>() {
                        {
                                put("recipientIds", Arrays.asList(regularUser.getId()));
                                put("subject", "");
                                put("messageBody", "Test message body");
                        }
                });

                mockMvc.perform(post("/admin/send-bulk-email")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.status").value("error"))
                                .andExpect(jsonPath("$.message").value("Subject cannot be empty"));
        }

        @Test
        @WithMockUser(username = "tarparakrimy1@gmail.com", roles = "ADMIN")
        void sendBulkEmail_WithEmptyMessageBody_ShouldReturnBadRequest() throws Exception {
                String requestJson = objectMapper.writeValueAsString(new java.util.HashMap<String, Object>() {
                        {
                                put("recipientIds", Arrays.asList(regularUser.getId()));
                                put("subject", "Test Subject");
                                put("messageBody", "");
                        }
                });

                mockMvc.perform(post("/admin/send-bulk-email")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.status").value("error"))
                                .andExpect(jsonPath("$.message").value("Message body cannot be empty"));
        }

        @Test
        @WithMockUser(username = "tarparakrimy1@gmail.com", roles = "ADMIN")
        void sendBulkEmail_WithWhitespaceOnlySubject_ShouldReturnBadRequest() throws Exception {
                String requestJson = objectMapper.writeValueAsString(new java.util.HashMap<String, Object>() {
                        {
                                put("recipientIds", Arrays.asList(regularUser.getId()));
                                put("subject", "   ");
                                put("messageBody", "Valid message");
                        }
                });

                mockMvc.perform(post("/admin/send-bulk-email")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.status").value("error"))
                                .andExpect(jsonPath("$.message").value("Subject cannot be empty"));
        }

        @Test
        @WithMockUser(username = "tarparakrimy1@gmail.com", roles = "ADMIN")
        void sendBulkEmail_WithSingleRecipient_ShouldSendSuccessfully() throws Exception {
                String requestJson = objectMapper.writeValueAsString(new java.util.HashMap<String, Object>() {
                        {
                                put("recipientIds", Arrays.asList(regularUser.getId()));
                                put("subject", "Single Recipient Test");
                                put("messageBody", "This email is sent to a single recipient.");
                        }
                });

                mockMvc.perform(post("/admin/send-bulk-email")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("success"))
                                .andExpect(jsonPath("$.successCount").value("1"))
                                .andExpect(jsonPath("$.failureCount").value("0"))
                                .andExpect(jsonPath("$.message")
                                                .value(containsString("Emails sent successfully to 1 recipient(s)")));
        }

        @Test
        @WithMockUser(username = "tarparakrimy1@gmail.com", roles = "ADMIN")
        void sendBulkEmail_WithNonExistentRecipient_ShouldHandleGracefully() throws Exception {
                // Mix of valid and invalid recipient IDs
                String requestJson = objectMapper.writeValueAsString(new java.util.HashMap<String, Object>() {
                        {
                                put("recipientIds", Arrays.asList(regularUser.getId(), 99999)); // 99999 doesn't exist
                                put("subject", "Test Subject");
                                put("messageBody", "Test message");
                        }
                });

                mockMvc.perform(post("/admin/send-bulk-email")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("success"))
                                .andExpect(jsonPath("$.successCount").value("1"))
                                .andExpect(jsonPath("$.failureCount").value("1"))
                                .andExpect(jsonPath("$.message")
                                                .value(containsString("Failed to send to 1 recipient(s)")));
        }

        @Test
        @WithMockUser(username = "user@example.com", roles = "USER")
        void sendBulkEmail_WithUserRole_ShouldBeForbidden() throws Exception {
                String requestJson = objectMapper.writeValueAsString(new java.util.HashMap<String, Object>() {
                        {
                                put("recipientIds", Arrays.asList(regularUser.getId()));
                                put("subject", "Test Subject");
                                put("messageBody", "Test message");
                        }
                });

                mockMvc.perform(post("/admin/send-bulk-email")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson))
                                .andExpect(status().isForbidden());
        }

        @Test
        void sendBulkEmail_WithoutAuth_ShouldReturnUnauthorized() throws Exception {
                String requestJson = objectMapper.writeValueAsString(new java.util.HashMap<String, Object>() {
                        {
                                put("recipientIds", Arrays.asList(1));
                                put("subject", "Test Subject");
                                put("messageBody", "Test message");
                        }
                });

                mockMvc.perform(post("/admin/send-bulk-email")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson))
                                .andExpect(status().is3xxRedirection());
        }

        // ========================== Selection Behavior Integration Tests
        // ==========================

        @Test
        @WithMockUser(username = "tarparakrimy1@gmail.com", roles = "ADMIN")
        void bulkEmail_WithMultipleSelections_ShouldProcessAllRecipients() throws Exception {
                // Create multiple users to simulate selection behavior
                final User user1 = userRepository.save(User.builder()
                                .firstName("Alice")
                                .lastName("Williams")
                                .email("alice@example.com")
                                .password("password")
                                .role(Role.USER.name())
                                .creation(new Date())
                                .build());

                final User user2 = userRepository.save(User.builder()
                                .firstName("Charlie")
                                .lastName("Brown")
                                .email("charlie@example.com")
                                .password("password")
                                .role(Role.USER.name())
                                .creation(new Date())
                                .build());

                final User user3 = userRepository.save(User.builder()
                                .firstName("Diana")
                                .lastName("Davis")
                                .email("diana@example.com")
                                .password("password")
                                .role(Role.USER.name())
                                .creation(new Date())
                                .build());

                // Simulate selecting multiple checkboxes (select all scenario)
                List<Integer> selectedIds = Arrays.asList(
                                regularUser.getId(),
                                user1.getId(),
                                user2.getId(),
                                user3.getId());

                String requestJson = objectMapper.writeValueAsString(new java.util.HashMap<String, Object>() {
                        {
                                put("recipientIds", selectedIds);
                                put("subject", "Select All Test");
                                put("messageBody", "Testing bulk email with all members selected");
                        }
                });

                mockMvc.perform(post("/admin/send-bulk-email")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("success"))
                                .andExpect(jsonPath("$.successCount").value("4"))
                                .andExpect(jsonPath("$.message")
                                                .value(containsString("Emails sent successfully to 4 recipient(s)")));
        }

        @Test
        @WithMockUser(username = "tarparakrimy1@gmail.com", roles = "ADMIN")
        void bulkEmail_WithPartialSelection_ShouldProcessOnlySelected() throws Exception {
                // Create multiple users
                final User user1 = userRepository.save(User.builder()
                                .firstName("Eve")
                                .lastName("Martinez")
                                .email("eve@example.com")
                                .password("password")
                                .role(Role.USER.name())
                                .creation(new Date())
                                .build());

                final User user2 = userRepository.save(User.builder()
                                .firstName("Frank")
                                .lastName("Garcia")
                                .email("frank@example.com")
                                .password("password")
                                .role(Role.USER.name())
                                .creation(new Date())
                                .build());

                // Simulate selecting only 2 out of 3 available users
                List<Integer> selectedIds = Arrays.asList(regularUser.getId(), user1.getId());

                String requestJson = objectMapper.writeValueAsString(new java.util.HashMap<String, Object>() {
                        {
                                put("recipientIds", selectedIds);
                                put("subject", "Partial Selection Test");
                                put("messageBody", "Testing bulk email with partial selection");
                        }
                });

                mockMvc.perform(post("/admin/send-bulk-email")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("success"))
                                .andExpect(jsonPath("$.successCount").value("2"))
                                .andExpect(jsonPath("$.failureCount").value("0"));
        }

        @Test
        @WithMockUser(username = "tarparakrimy1@gmail.com", roles = "ADMIN")
        void bulkEmail_WithDeselectAll_ShouldReturnBadRequest() throws Exception {
                // Simulate deselecting all checkboxes (empty selection)
                String requestJson = objectMapper.writeValueAsString(new java.util.HashMap<String, Object>() {
                        {
                                put("recipientIds", Collections.emptyList());
                                put("subject", "Deselect All Test");
                                put("messageBody", "This should not be sent");
                        }
                });

                mockMvc.perform(post("/admin/send-bulk-email")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.status").value("error"))
                                .andExpect(jsonPath("$.message").value("No recipients selected"));
        }

        @Test
        @WithMockUser(username = "tarparakrimy1@gmail.com", roles = "ADMIN")
        void bulkEmail_WithToggleSelectAll_ShouldProcessCorrectly() throws Exception {
                // Create users
                final User user1 = userRepository.save(User.builder()
                                .firstName("Grace")
                                .lastName("Lee")
                                .email("grace@example.com")
                                .password("password")
                                .role(Role.USER.name())
                                .creation(new Date())
                                .build());

                final User user2 = userRepository.save(User.builder()
                                .firstName("Henry")
                                .lastName("Wilson")
                                .email("henry@example.com")
                                .password("password")
                                .role(Role.USER.name())
                                .creation(new Date())
                                .build());

                // First: Select all (toggle on)
                List<Integer> allSelected = Arrays.asList(regularUser.getId(), user1.getId(), user2.getId());
                String requestJson1 = objectMapper.writeValueAsString(new java.util.HashMap<String, Object>() {
                        {
                                put("recipientIds", allSelected);
                                put("subject", "Toggle Select All - On");
                                put("messageBody", "All members selected");
                        }
                });

                mockMvc.perform(post("/admin/send-bulk-email")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson1))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.successCount").value("3"));

                // Second: Deselect all (toggle off) - should fail
                String requestJson2 = objectMapper.writeValueAsString(new java.util.HashMap<String, Object>() {
                        {
                                put("recipientIds", Collections.emptyList());
                                put("subject", "Toggle Select All - Off");
                                put("messageBody", "No members selected");
                        }
                });

                mockMvc.perform(post("/admin/send-bulk-email")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson2))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("No recipients selected"));
        }

        @Test
        @WithMockUser(username = "tarparakrimy1@gmail.com", roles = "ADMIN")
        void bulkEmail_VerifiesRecipientIdsExistInDatabase() throws Exception {
                // Create one valid user
                final User validUser = userRepository.save(User.builder()
                                .firstName("Valid")
                                .lastName("User")
                                .email("valid@example.com")
                                .password("password")
                                .role(Role.USER.name())
                                .creation(new Date())
                                .build());

                // Submit request with mix of valid and invalid IDs
                List<Integer> mixedIds = Arrays.asList(validUser.getId(), 88888, 99999);
                String requestJson = objectMapper.writeValueAsString(new java.util.HashMap<String, Object>() {
                        {
                                put("recipientIds", mixedIds);
                                put("subject", "Database Validation Test");
                                put("messageBody", "Testing recipient validation");
                        }
                });

                mockMvc.perform(post("/admin/send-bulk-email")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.successCount").value("1"))
                                .andExpect(jsonPath("$.failureCount").value("2"))
                                .andExpect(jsonPath("$.message")
                                                .value(containsString("Failed to send to 2 recipient(s)")));
        }
}