package org.voice.membership.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.MediaType;
import org.voice.membership.config.TestEmailConfig;
import org.voice.membership.entities.Role;
import org.voice.membership.entities.User;
import org.voice.membership.repositories.LandingPageContentRepository;
import org.voice.membership.repositories.MembershipRepository;
import org.voice.membership.repositories.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Calendar;
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
@Import(TestEmailConfig.class)
class AdminControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private LandingPageContentRepository landingPageContentRepository;

        @Autowired
        private MembershipRepository membershipRepository;

        @Autowired
        private ObjectMapper objectMapper;

        @Autowired
        private org.voice.membership.services.LandingPageService landingPageService;

        private User adminUser;
        private User regularUser;
        private org.voice.membership.entities.Membership freeMembership;
        private org.voice.membership.entities.Membership premiumMembership;

        @BeforeEach
        void setUp() {
                userRepository.deleteAll();
                membershipRepository.deleteAll();

                // Create test memberships
                freeMembership = org.voice.membership.entities.Membership.builder()
                                .name("Free")
                                .description("Free membership")
                                .price(new java.math.BigDecimal("0.00"))
                                .features("Basic features")
                                .isFree(true)
                                .displayOrder(1)
                                .active(true)
                                .build();
                freeMembership = membershipRepository.save(freeMembership);

                premiumMembership = org.voice.membership.entities.Membership.builder()
                                .name("Premium")
                                .description("Premium membership with all features")
                                .price(new java.math.BigDecimal("20.00"))
                                .features("- All features\n- Priority support")
                                .isFree(false)
                                .displayOrder(2)
                                .active(true)
                                .build();
                premiumMembership = membershipRepository.save(premiumMembership);

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
                                .city("Toronto")
                                .province("ON")
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
                                .andExpect(jsonPath("$.status").value("success"))
                                .andExpect(jsonPath("$.data.id").value(regularUser.getId()))
                                .andExpect(jsonPath("$.data.firstName").value("Regular"))
                                .andExpect(jsonPath("$.data.lastName").value("User"))
                                .andExpect(jsonPath("$.data.email").value("user@example.com"))
                                .andExpect(jsonPath("$.data.city").value("Toronto"))
                                .andExpect(jsonPath("$.data.province").value("ON"));
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
        void updateMember_WithBlankPostalCode_ShouldUpdateAndRedirect() throws Exception {
                mockMvc.perform(post("/admin/edit-member/" + regularUser.getId())
                                .with(csrf())
                                .param("userId", String.valueOf(regularUser.getId()))
                                .param("firstName", "UpdatedFirst")
                                .param("lastName", "UpdatedLast")
                                .param("email", "user@example.com")
                                .param("phone", "9876543210")
                                .param("address", "")
                                .param("city", "")
                                .param("province", "")
                                .param("postalCode", "")
                                .param("emailVerified", "true")
                                .param("accountLocked", "false"))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/admin/dashboard"));

                User updated = userRepository.findById(regularUser.getId()).orElse(null);
                assertThat(updated).isNotNull();
                assertThat(updated.getPostalCode()).isEmpty();
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
                                .andExpect(status().isOk())
                                .andExpect(view().name("admin-add-member"));
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
                                .andExpect(status().isOk())
                                .andExpect(view().name("admin-add-member"));
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
                                .andExpect(status().isOk())
                                .andExpect(view().name("admin-add-admin"));
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
                mockMvc.perform(get("/api/admin/notifications/unread"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("success"))
                                .andExpect(jsonPath("$.data").isArray());
        }

        @Test
        @WithMockUser(username = "tarparakrimy1@gmail.com", roles = "ADMIN")
        void getUnreadNotificationsCount_ShouldReturnCount() throws Exception {
                mockMvc.perform(get("/api/admin/notifications/count"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("success"))
                                .andExpect(jsonPath("$.data.count").isNumber());
        }

        @Test
        @WithMockUser(username = "tarparakrimy1@gmail.com", roles = "ADMIN")
        void dismissAllNotifications_ShouldSucceed() throws Exception {
                mockMvc.perform(post("/api/admin/notifications/dismiss-all")
                                .with(csrf()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("success"));
        }

        @Test
        @WithMockUser(username = "user@example.com", roles = "USER")
        void getUnreadNotifications_WithUserRole_ShouldBeForbidden() throws Exception {
                mockMvc.perform(get("/api/admin/notifications/unread"))
                                .andExpect(status().isForbidden());
        }

        @Test
        void getUnreadNotifications_WithoutAuth_ShouldRedirectToLogin() throws Exception {
                mockMvc.perform(get("/api/admin/notifications/unread"))
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
                                .andExpect(jsonPath("$.data.successCount").value(3))
                                .andExpect(jsonPath("$.data.failureCount").value(0))
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
                                .andExpect(jsonPath("$.message").value("Validation failed"))
                                .andExpect(jsonPath("$.data.fieldErrors.recipientIds").exists());
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
                                .andExpect(jsonPath("$.message").value("Validation failed"))
                                .andExpect(jsonPath("$.data.fieldErrors.subject").exists());
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
                                .andExpect(jsonPath("$.message").value("Validation failed"))
                                .andExpect(jsonPath("$.data.fieldErrors.messageBody").exists());
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
                                .andExpect(jsonPath("$.message").value("Validation failed"))
                                .andExpect(jsonPath("$.data.fieldErrors.subject").exists());
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
                                .andExpect(jsonPath("$.data.successCount").value(1))
                                .andExpect(jsonPath("$.data.failureCount").value(0))
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
                                .andExpect(jsonPath("$.data.successCount").value(1))
                                .andExpect(jsonPath("$.data.failureCount").value(1))
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
                                .andExpect(jsonPath("$.data.successCount").value(4))
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
                                .andExpect(jsonPath("$.data.successCount").value(2))
                                .andExpect(jsonPath("$.data.failureCount").value(0));
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
                                .andExpect(jsonPath("$.message").value("Validation failed"))
                                .andExpect(jsonPath("$.data.fieldErrors.recipientIds").exists());
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
                                .andExpect(jsonPath("$.data.successCount").value(3));

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
                                .andExpect(jsonPath("$.message").value("Validation failed"))
                                .andExpect(jsonPath("$.data.fieldErrors.recipientIds").exists());
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
                                .andExpect(jsonPath("$.data.successCount").value(1))
                                .andExpect(jsonPath("$.data.failureCount").value(2))
                                .andExpect(jsonPath("$.message")
                                                .value(containsString("Failed to send to 2 recipient(s)")));
        }

        // ==================== Edit Membership Renewal Reminder Email Tests
        // ====================
        // Scenario: Admin wants to edit the membership renewal reminder email

        @Test
        @WithMockUser(username = "tarparakrimy1@gmail.com", roles = "ADMIN")
        void renewalEmailEditor_WithAdminRole_ShouldReturnEditorForm() throws Exception {
                mockMvc.perform(get("/admin/renewal-email"))
                                .andExpect(status().isOk())
                                .andExpect(view().name("admin-renewal-email"))
                                .andExpect(model().attributeExists("renewalSubject"))
                                .andExpect(model().attributeExists("renewalBody"))
                                .andExpect(model().attributeExists("adminName"));
        }

        @Test
        @WithMockUser(username = "tarparakrimy1@gmail.com", roles = "ADMIN")
        void renewalEmailEditor_ShouldLoadCurrentEmailContent() throws Exception {
                mockMvc.perform(get("/admin/renewal-email"))
                                .andExpect(status().isOk())
                                .andExpect(model().attributeExists("renewalSubject"))
                                .andExpect(model().attributeExists("renewalBody"));
        }

        @Test
        @WithMockUser(username = "tarparakrimy1@gmail.com", roles = "ADMIN")
        void saveRenewalEmail_WithValidSubjectAndBody_ShouldUpdate() throws Exception {
                String newSubject = "Your VOICE Membership Renews Soon - Action Required";
                String newBody = "Dear {memberName},\n\nYour VOICE membership expires on {expiryDate}. "
                                + "Please renew today: {renewalUrl}\n\nThank you!";

                mockMvc.perform(post("/admin/renewal-email/save")
                                .with(csrf())
                                .param("renewalSubject", newSubject)
                                .param("renewalBody", newBody))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/admin/renewal-email"))
                                .andExpect(flash().attributeExists("success"));

                // Verify the content was updated
                mockMvc.perform(get("/admin/renewal-email"))
                                .andExpect(status().isOk())
                                .andExpect(model().attribute("renewalSubject", newSubject))
                                .andExpect(model().attribute("renewalBody", newBody));
        }

        @Test
        @WithMockUser(username = "tarparakrimy1@gmail.com", roles = "ADMIN")
        void saveRenewalEmail_WithUpdatedSubjectOnly_ShouldUpdateSubject() throws Exception {
                String originalBody = landingPageService.getRenewalEmailBody();
                String newSubject = "Renew Your VOICE Membership Today";

                mockMvc.perform(post("/admin/renewal-email/save")
                                .with(csrf())
                                .param("renewalSubject", newSubject)
                                .param("renewalBody", originalBody))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/admin/renewal-email"))
                                .andExpect(flash().attributeExists("success"));

                // Verify subject was updated
                mockMvc.perform(get("/admin/renewal-email"))
                                .andExpect(status().isOk())
                                .andExpect(model().attribute("renewalSubject", newSubject));
        }

        @Test
        @WithMockUser(username = "tarparakrimy1@gmail.com", roles = "ADMIN")
        void saveRenewalEmail_WithUpdatedBodyOnly_ShouldUpdateBody() throws Exception {
                String originalSubject = landingPageService.getRenewalEmailSubject();
                String newBody = "Hello {memberName},\n\nIt's time to renew your membership before {expiryDate}. "
                                + "Renew now and continue enjoying VOICE benefits!\n\nRenew: {renewalUrl}\n\nBest regards,\nVOICE Team";

                mockMvc.perform(post("/admin/renewal-email/save")
                                .with(csrf())
                                .param("renewalSubject", originalSubject)
                                .param("renewalBody", newBody))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/admin/renewal-email"))
                                .andExpect(flash().attributeExists("success"));

                // Verify body was updated
                mockMvc.perform(get("/admin/renewal-email"))
                                .andExpect(status().isOk())
                                .andExpect(model().attribute("renewalBody", newBody));
        }

        @Test
        @WithMockUser(username = "tarparakrimy1@gmail.com", roles = "ADMIN")
        void saveRenewalEmail_WithTemplateVariables_ShouldPreserveVariables() throws Exception {
                String subjectWithVariables = "Your Membership Expires in {daysUntilExpiry} Day(s) - Renew Now";
                String bodyWithVariables = "Dear {memberName},\n\n"
                                + "Your {membershipName} expires on {expiryDate}.\n"
                                + "Days until expiry: {daysUntilExpiry}\n"
                                + "Renew here: {renewalUrl}\n\n"
                                + "Thank you for being a VOICE member!";

                mockMvc.perform(post("/admin/renewal-email/save")
                                .with(csrf())
                                .param("renewalSubject", subjectWithVariables)
                                .param("renewalBody", bodyWithVariables))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(flash().attributeExists("success"));

                // Verify variables were preserved
                mockMvc.perform(get("/admin/renewal-email"))
                                .andExpect(status().isOk())
                                .andExpect(model().attribute("renewalSubject",
                                                containsString("{daysUntilExpiry}")))
                                .andExpect(model().attribute("renewalBody",
                                                allOf(containsString("{memberName}"),
                                                                containsString("{expiryDate}"),
                                                                containsString("{renewalUrl}"))));
        }

        @Test
        @WithMockUser(username = "tarparakrimy1@gmail.com", roles = "ADMIN")
        void saveRenewalEmail_WithWhitespaceContent_ShouldTrimValues() throws Exception {
                String subjectWithWhitespace = "  \n  Renew Your Membership  \n  ";
                String bodyWithWhitespace = "  \n  Dear member,\n\n  Please renew.  \n  ";

                mockMvc.perform(post("/admin/renewal-email/save")
                                .with(csrf())
                                .param("renewalSubject", subjectWithWhitespace)
                                .param("renewalBody", bodyWithWhitespace))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(flash().attributeExists("success"));

                // Verify content was trimmed
                mockMvc.perform(get("/admin/renewal-email"))
                                .andExpect(status().isOk())
                                .andExpect(model().attribute("renewalSubject", "Renew Your Membership"))
                                .andExpect(model().attribute("renewalBody", "Dear member,\n\n  Please renew."));
        }

        @Test
        @WithMockUser(username = "tarparakrimy1@gmail.com", roles = "ADMIN")
        void saveRenewalEmail_WithMultilineBody_ShouldPreserveFormatting() throws Exception {
                String multilineBody = "Dear {memberName},\n\n"
                                + "This is a reminder that your membership expires on {expiryDate}.\n\n"
                                + "Benefits of renewing:\n"
                                + "- Access to all features\n"
                                + "- Priority support\n"
                                + "- Exclusive webinars\n\n"
                                + "Renew now: {renewalUrl}\n\n"
                                + "Best regards,\nThe VOICE Team";

                mockMvc.perform(post("/admin/renewal-email/save")
                                .with(csrf())
                                .param("renewalSubject", "Renew Your Membership")
                                .param("renewalBody", multilineBody))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(flash().attributeExists("success"));

                // Verify multiline formatting was preserved
                mockMvc.perform(get("/admin/renewal-email"))
                                .andExpect(status().isOk())
                                .andExpect(model().attribute("renewalBody", containsString("Benefits of renewing:")));
        }

        @Test
        @WithMockUser(username = "tarparakrimy1@gmail.com", roles = "ADMIN")
        void saveRenewalEmail_WithHTMLContent_ShouldAcceptHTMLTags() throws Exception {
                String htmlBody = "<p>Dear {memberName},</p>"
                                + "<p>Your membership expires on <strong>{expiryDate}</strong>.</p>"
                                + "<p>Please renew: <a href='{renewalUrl}'>Renew Here</a></p>";

                mockMvc.perform(post("/admin/renewal-email/save")
                                .with(csrf())
                                .param("renewalSubject", "Renew Your Membership Now")
                                .param("renewalBody", htmlBody))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(flash().attributeExists("success"));

                mockMvc.perform(get("/admin/renewal-email"))
                                .andExpect(status().isOk())
                                .andExpect(model().attribute("renewalBody", containsString("<p>")));
        }

        @Test
        @WithMockUser(username = "tarparakrimy1@gmail.com", roles = "ADMIN")
        void saveRenewalEmail_WithSpecialCharacters_ShouldPreserveCharacters() throws Exception {
                String subjectWithSpecialChars = "Your VOICE® Membership Renewal — {daysUntilExpiry} Day(s) Left!";
                String bodyWithSpecialChars = "© 2026 VOICE Organization\n\n"
                                + "Dear {memberName},\n\n"
                                + "Your membership ends on {expiryDate} — don't miss out!\n\n"
                                + "Prices: Free / $20/year • Sign-up bonus: 10%";

                mockMvc.perform(post("/admin/renewal-email/save")
                                .with(csrf())
                                .param("renewalSubject", subjectWithSpecialChars)
                                .param("renewalBody", bodyWithSpecialChars))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(flash().attributeExists("success"));

                mockMvc.perform(get("/admin/renewal-email"))
                                .andExpect(status().isOk())
                                .andExpect(model().attribute("renewalSubject",
                                                containsString("VOICE®")))
                                .andExpect(model().attribute("renewalBody",
                                                containsString("© 2026")));
        }

        @Test
        @WithMockUser(username = "tarparakrimy1@gmail.com", roles = "ADMIN")
        void saveRenewalEmail_WithLongContent_ShouldAcceptLongText() throws Exception {
                StringBuilder longBody = new StringBuilder();
                longBody.append("Dear {memberName},\n\n");
                for (int i = 1; i <= 10; i++) {
                        longBody.append("Paragraph ").append(i).append(": ")
                                        .append("Lorem ipsum dolor sit amet, consectetur adipiscing elit. ")
                                        .append("Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. ")
                                        .append("Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris.\n\n");
                }
                longBody.append("Renew at: {renewalUrl}");

                mockMvc.perform(post("/admin/renewal-email/save")
                                .with(csrf())
                                .param("renewalSubject", "Your Membership Renewal Information")
                                .param("renewalBody", longBody.toString()))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(flash().attributeExists("success"));

                mockMvc.perform(get("/admin/renewal-email"))
                                .andExpect(status().isOk())
                                .andExpect(model().attribute("renewalBody",
                                                containsString("Paragraph 10")));
        }

        @Test
        @WithMockUser(username = "tarparakrimy1@gmail.com", roles = "ADMIN")
        void saveRenewalEmail_MultipleUpdates_ShouldPersistLastUpdate() throws Exception {
                // First update
                mockMvc.perform(post("/admin/renewal-email/save")
                                .with(csrf())
                                .param("renewalSubject", "First Update Subject")
                                .param("renewalBody", "First update body"))
                                .andExpect(status().is3xxRedirection());

                // Second update
                mockMvc.perform(post("/admin/renewal-email/save")
                                .with(csrf())
                                .param("renewalSubject", "Second Update Subject")
                                .param("renewalBody", "Second update body"))
                                .andExpect(status().is3xxRedirection());

                // Verify only the second update persists
                mockMvc.perform(get("/admin/renewal-email"))
                                .andExpect(status().isOk())
                                .andExpect(model().attribute("renewalSubject", "Second Update Subject"))
                                .andExpect(model().attribute("renewalBody", "Second update body"));
        }

        @Test
        void renewalEmailEditor_WithoutAuthentication_ShouldRedirectToLogin() throws Exception {
                mockMvc.perform(get("/admin/renewal-email"))
                                .andExpect(status().is3xxRedirection());
        }

        @Test
        void saveRenewalEmail_WithoutAuthentication_ShouldRedirectToLogin() throws Exception {
                mockMvc.perform(post("/admin/renewal-email/save")
                                .with(csrf())
                                .param("renewalSubject", "Subject")
                                .param("renewalBody", "Body"))
                                .andExpect(status().is3xxRedirection());
        }

        @Test
        @WithMockUser(username = "user@example.com", roles = "USER")
        void renewalEmailEditor_WithUserRole_ShouldBeForbidden() throws Exception {
                mockMvc.perform(get("/admin/renewal-email"))
                                .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "user@example.com", roles = "USER")
        void saveRenewalEmail_WithUserRole_ShouldBeForbidden() throws Exception {
                mockMvc.perform(post("/admin/renewal-email/save")
                                .with(csrf())
                                .param("renewalSubject", "Subject")
                                .param("renewalBody", "Body"))
                                .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "tarparakrimy1@gmail.com", roles = "ADMIN")
        void saveRenewalEmail_WithoutCSRF_ShouldBeForbidden() throws Exception {
                mockMvc.perform(post("/admin/renewal-email/save")
                                .param("renewalSubject", "Subject")
                                .param("renewalBody", "Body"))
                                .andExpect(status().isForbidden());
        }

        // ==================== Renewal Reminder Endpoint Tests ====================

        @Test
        @WithMockUser(username = "tarparakrimy1@gmail.com", roles = "ADMIN")
        void triggerRenewalReminders_WithNoExpiringMembers_ShouldReturnSuccessWithZeroCounts() throws Exception {
                // No paid members in the test DB, so all window counts should be 0
                mockMvc.perform(post("/admin/trigger-renewal-reminders")
                                .with(csrf()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("success"))
                                .andExpect(jsonPath("$.data.totalMembersFound").value(0))
                                .andExpect(jsonPath("$.data.totalEmailsSent").value(0))
                                .andExpect(jsonPath("$.data.totalEmailsFailed").value(0))
                                .andExpect(jsonPath("$.data.windows").exists());
        }

        @Test
        @WithMockUser(username = "tarparakrimy1@gmail.com", roles = "ADMIN")
        void previewRenewalReminders_WithExpiringMember_ShouldReturnMemberDetails() throws Exception {
                // Arrange: find the paid membership plan and create a paid member expiring in 5
                // days
                var paidMembership = membershipRepository.findAll().stream()
                                .filter(m -> !m.isFree())
                                .findFirst()
                                .orElse(null);

                if (paidMembership != null) {
                        Calendar cal = Calendar.getInstance();
                        cal.add(Calendar.DAY_OF_MONTH, 5);

                        User expiringMember = User.builder()
                                        .firstName("Expiring")
                                        .lastName("Member")
                                        .email("expiring@example.com")
                                        .password("Test1234!")
                                        .role(Role.USER.name())
                                        .paid(true)
                                        .membership(paidMembership)
                                        .membershipExpiryDate(cal.getTime())
                                        .membershipStartDate(new Date())
                                        .creation(new Date())
                                        .build();
                        userRepository.save(expiringMember);

                        mockMvc.perform(get("/admin/renewal-reminders/preview")
                                        .param("withinDays", "10"))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.status").value("success"))
                                        .andExpect(jsonPath("$.data.withinDays").value(10))
                                        .andExpect(jsonPath("$.data.membersFound").value(greaterThanOrEqualTo(1)))
                                        .andExpect(jsonPath("$.data.members[0].email").value("expiring@example.com"))
                                        .andExpect(jsonPath("$.data.members[0].paid").value(true))
                                        .andExpect(jsonPath("$.data.note").exists());
                }
        }

        @Test
        @WithMockUser(username = "tarparakrimy1@gmail.com", roles = "ADMIN")
        void previewRenewalReminders_WithNoExpiringMembers_ShouldReturnEmptyList() throws Exception {
                mockMvc.perform(get("/admin/renewal-reminders/preview")
                                .param("withinDays", "5"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("success"))
                                .andExpect(jsonPath("$.data.membersFound").value(0))
                                .andExpect(jsonPath("$.data.members").isArray());
        }

        @Test
        @WithMockUser(username = "user@example.com", roles = "USER")
        void triggerRenewalReminders_WithNonAdminUser_ShouldBeForbidden() throws Exception {
                mockMvc.perform(post("/admin/trigger-renewal-reminders")
                                .with(csrf()))
                                .andExpect(status().isForbidden());
        }

        // ========================== Edit Landing Page Tests ==========================

        @Test
        @WithMockUser(username = "tarparakrimy1@gmail.com", roles = "ADMIN")
        void landingPageEditor_WithAdminRole_ShouldReturnEditorForm() throws Exception {
                mockMvc.perform(get("/admin/landing-page"))
                                .andExpect(status().isOk())
                                .andExpect(view().name("admin-landing-page"))
                                .andExpect(model().attributeExists("heroTitle"))
                                .andExpect(model().attributeExists("heroTagline"))
                                .andExpect(model().attributeExists("benefitsTitle"))
                                .andExpect(model().attributeExists("reasonsHeading"))
                                .andExpect(model().attributeExists("reasonsContent"))
                                .andExpect(model().attributeExists("adminName"));
        }

        // ========================== Edit Membership Plan Details Tests
        // ==========================
        // Scenario: Admin wants to edit existing membership plan details

        @Test
        @WithMockUser(username = "tarparakrimy1@gmail.com", roles = "ADMIN")
        void editMembershipsPage_WithAdminRole_ShouldShowMemberships() throws Exception {
                mockMvc.perform(get("/admin/memberships"))
                                .andExpect(status().isOk())
                                .andExpect(view().name("admin-edit-memberships"))
                                .andExpect(model().attributeExists("memberships"))
                                .andExpect(model().attributeExists("adminName"));
        }

        @Test
        @WithMockUser(username = "tarparakrimy1@gmail.com", roles = "ADMIN")
        void editMembershipsPage_ShouldDisplayAllActiveMemberships() throws Exception {
                mockMvc.perform(get("/admin/memberships"))
                                .andExpect(status().isOk())
                                .andExpect(model().attribute("memberships", hasSize(2)));
        }

        @Test
        @WithMockUser(username = "tarparakrimy1@gmail.com", roles = "ADMIN")
        void saveMembership_WithValidPremiumData_ShouldUpdatePricing() throws Exception {
                mockMvc.perform(post("/admin/memberships/" + premiumMembership.getId() + "/save")
                                .with(csrf())
                                .param("name", "Premium Plus")
                                .param("description", "Enhanced premium membership with priority support")
                                .param("price", "29.99")
                                .param("features",
                                                "- All premium features\n- 24/7 Priority support\n- Monthly webinars"))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/admin/memberships"))
                                .andExpect(flash().attributeExists("success"));

                org.voice.membership.entities.Membership updated = membershipRepository
                                .findById(premiumMembership.getId()).orElse(null);
                assertThat(updated).isNotNull();
                assertThat(updated.getName()).isEqualTo("Premium Plus");
                assertThat(updated.getPrice()).isEqualByComparingTo(new BigDecimal("29.99"));
                assertThat(updated.getDescription())
                                .isEqualTo("Enhanced premium membership with priority support");
        }

        @Test
        @WithMockUser(username = "tarparakrimy1@gmail.com", roles = "ADMIN")
        void saveMembership_WithUpdatedDescription_ShouldUpdateDescriptionOnly() throws Exception {
                String oldPrice = premiumMembership.getPrice().toString();

                mockMvc.perform(post("/admin/memberships/" + premiumMembership.getId() + "/save")
                                .with(csrf())
                                .param("name", "Premium")
                                .param("description", "Updated description for premium membership")
                                .param("price", oldPrice)
                                .param("features", premiumMembership.getFeatures()))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/admin/memberships"))
                                .andExpect(flash().attributeExists("success"));

                org.voice.membership.entities.Membership updated = membershipRepository
                                .findById(premiumMembership.getId()).orElse(null);
                assertThat(updated).isNotNull();
                assertThat(updated.getDescription()).isEqualTo("Updated description for premium membership");
        }

        @Test
        @WithMockUser(username = "tarparakrimy1@gmail.com", roles = "ADMIN")
        void saveMembership_WithUpdatedFeatures_ShouldUpdateFeatures() throws Exception {
                String newFeatures = "- Feature 1\n- Feature 2\n- Feature 3\n- New Feature 4";

                mockMvc.perform(post("/admin/memberships/" + premiumMembership.getId() + "/save")
                                .with(csrf())
                                .param("name", "Premium")
                                .param("description", premiumMembership.getDescription())
                                .param("price", premiumMembership.getPrice().toString())
                                .param("features", newFeatures))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/admin/memberships"))
                                .andExpect(flash().attributeExists("success"));

                org.voice.membership.entities.Membership updated = membershipRepository
                                .findById(premiumMembership.getId()).orElse(null);
                assertThat(updated).isNotNull();
                assertThat(updated.getFeatures()).contains("Feature 1", "Feature 2", "Feature 3", "New Feature 4");
        }

        @Test
        @WithMockUser(username = "tarparakrimy1@gmail.com", roles = "ADMIN")
        void saveMembership_FreeMembership_ShouldNotChangePriceFromZero() throws Exception {
                mockMvc.perform(post("/admin/memberships/" + freeMembership.getId() + "/save")
                                .with(csrf())
                                .param("name", "Free Basic")
                                .param("description", "Updated free membership description")
                                .param("price", "99.99")
                                .param("features", "- Basic features only"))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/admin/memberships"))
                                .andExpect(flash().attributeExists("success"));

                org.voice.membership.entities.Membership updated = membershipRepository
                                .findById(freeMembership.getId()).orElse(null);
                assertThat(updated).isNotNull();
                assertThat(updated.isFree()).isTrue();
                assertThat(updated.getPrice()).isEqualByComparingTo(new BigDecimal("0.00"));
        }

        @Test
        @WithMockUser(username = "tarparakrimy1@gmail.com", roles = "ADMIN")
        void saveMembership_WithInvalidPrice_ShouldShowErrorMessage() throws Exception {
                mockMvc.perform(post("/admin/memberships/" + premiumMembership.getId() + "/save")
                                .with(csrf())
                                .param("name", "Premium")
                                .param("description", premiumMembership.getDescription())
                                .param("price", "invalid-price")
                                .param("features", premiumMembership.getFeatures()))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/admin/memberships"))
                                .andExpect(flash().attribute("error", containsString("Invalid price format")));

                org.voice.membership.entities.Membership unchanged = membershipRepository
                                .findById(premiumMembership.getId()).orElse(null);
                assertThat(unchanged).isNotNull();
                assertThat(unchanged.getPrice()).isEqualByComparingTo(new BigDecimal("20.00"));
        }

        @Test
        @WithMockUser(username = "tarparakrimy1@gmail.com", roles = "ADMIN")
        void saveMembership_WithNegativePrice_ShouldHandleGracefully() throws Exception {
                mockMvc.perform(post("/admin/memberships/" + premiumMembership.getId() + "/save")
                                .with(csrf())
                                .param("name", "Premium")
                                .param("description", premiumMembership.getDescription())
                                .param("price", "-50.00")
                                .param("features", premiumMembership.getFeatures()))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/admin/memberships"));
        }

        @Test
        @WithMockUser(username = "tarparakrimy1@gmail.com", roles = "ADMIN")
        void saveMembership_WithNonexistentId_ShouldShowError() throws Exception {
                mockMvc.perform(post("/admin/memberships/99999/save")
                                .with(csrf())
                                .param("name", "Nonexistent")
                                .param("description", "This membership does not exist")
                                .param("price", "50.00")
                                .param("features", "Some features"))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/admin/memberships"))
                                .andExpect(flash().attribute("error", containsString("not found")));
        }

        @Test
        @WithMockUser(username = "tarparakrimy1@gmail.com", roles = "ADMIN")
        void saveMembership_WithWhitespaceInName_ShouldTrimAndSave() throws Exception {
                mockMvc.perform(post("/admin/memberships/" + premiumMembership.getId() + "/save")
                                .with(csrf())
                                .param("name", "  Premium Updated  ")
                                .param("description", "  Trimmed description  ")
                                .param("price", premiumMembership.getPrice().toString())
                                .param("features", premiumMembership.getFeatures()))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/admin/memberships"));

                org.voice.membership.entities.Membership updated = membershipRepository
                                .findById(premiumMembership.getId()).orElse(null);
                assertThat(updated).isNotNull();
                assertThat(updated.getName()).isEqualTo("Premium Updated");
                assertThat(updated.getDescription()).isEqualTo("Trimmed description");
        }

        @Test
        @WithMockUser(username = "tarparakrimy1@gmail.com", roles = "ADMIN")
        void saveMembership_WithLargePrice_ShouldAcceptDecimalValues() throws Exception {
                mockMvc.perform(post("/admin/memberships/" + premiumMembership.getId() + "/save")
                                .with(csrf())
                                .param("name", "Premium Enterprise")
                                .param("description", premiumMembership.getDescription())
                                .param("price", "999.99")
                                .param("features", premiumMembership.getFeatures()))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/admin/memberships"))
                                .andExpect(flash().attributeExists("success"));

                org.voice.membership.entities.Membership updated = membershipRepository
                                .findById(premiumMembership.getId()).orElse(null);
                assertThat(updated).isNotNull();
                assertThat(updated.getPrice()).isEqualByComparingTo(new BigDecimal("999.99"));
        }

        @Test
        @WithMockUser(username = "tarparakrimy1@gmail.com", roles = "ADMIN")
        void saveMembership_WithEmptyPrice_FreeMembership_ShouldNotUpdate() throws Exception {
                mockMvc.perform(post("/admin/memberships/" + premiumMembership.getId() + "/save")
                                .with(csrf())
                                .param("name", "Premium")
                                .param("description", premiumMembership.getDescription())
                                .param("price", "")
                                .param("features", premiumMembership.getFeatures()))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/admin/memberships"));
        }

        @Test
        @WithMockUser(username = "tarparakrimy1@gmail.com", roles = "ADMIN")
        void saveMembership_CompleteUpdate_ShouldUpdateAllFields() throws Exception {
                mockMvc.perform(post("/admin/memberships/" + premiumMembership.getId() + "/save")
                                .with(csrf())
                                .param("name", "Premium Gold")
                                .param("description", "Our most advanced membership tier")
                                .param("price", "49.99")
                                .param("features", "- Unlimited access\n- Premium support\n- Custom features"))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/admin/memberships"))
                                .andExpect(flash().attributeExists("success"));

                org.voice.membership.entities.Membership updated = membershipRepository
                                .findById(premiumMembership.getId()).orElse(null);
                assertThat(updated).isNotNull();
                assertThat(updated.getName()).isEqualTo("Premium Gold");
                assertThat(updated.getDescription()).isEqualTo("Our most advanced membership tier");
                assertThat(updated.getPrice()).isEqualByComparingTo(new BigDecimal("49.99"));
                assertThat(updated.getFeatures())
                                .contains("Unlimited access", "Premium support", "Custom features");
        }

        @Test
        void saveMembership_WithoutAuthentication_ShouldRedirectToLogin() throws Exception {
                mockMvc.perform(post("/admin/memberships/" + premiumMembership.getId() + "/save")
                                .with(csrf())
                                .param("name", "Premium")
                                .param("description", "Test")
                                .param("price", "20.00")
                                .param("features", "Test features"))
                                .andExpect(status().is3xxRedirection());
        }

        @Test
        @WithMockUser(username = "user@example.com", roles = "USER")
        void editMembershipsPage_WithUserRole_ShouldBeForbidden() throws Exception {
                mockMvc.perform(get("/admin/memberships"))
                                .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "user@example.com", roles = "USER")
        void saveMembership_WithUserRole_ShouldBeForbidden() throws Exception {
                mockMvc.perform(post("/admin/memberships/" + premiumMembership.getId() + "/save")
                                .with(csrf())
                                .param("name", "Premium")
                                .param("description", "Test")
                                .param("price", "29.99")
                                .param("features", "Test features"))
                                .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "tarparakrimy1@gmail.com", roles = "ADMIN")
        void saveLandingPage_WithValidData_ShouldRedirectWithSuccess() throws Exception {
                mockMvc.perform(post("/admin/landing-page/save")
                                .with(csrf())
                                .param("heroTitle", "Welcome to VOICE")
                                .param("heroTagline", "Empowering families of Deaf and Hard of Hearing children")
                                .param("benefitsTitle", "Why Join VOICE?")
                                .param("reasonsHeading", "10 Great Reasons to Join")
                                .param("reasonsContent", "<ol><li>Community support</li></ol>"))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/admin/landing-page"))
                                .andExpect(flash().attributeExists("success"));
        }

        @Test
        @WithMockUser(username = "tarparakrimy1@gmail.com", roles = "ADMIN")
        void saveLandingPage_ContentShouldBePersistedInDatabase() throws Exception {
                mockMvc.perform(post("/admin/landing-page/save")
                                .with(csrf())
                                .param("heroTitle", "Persisted Hero Title")
                                .param("heroTagline", "Persisted tagline text")
                                .param("benefitsTitle", "Persisted Benefits")
                                .param("reasonsHeading", "Persisted Reasons Heading")
                                .param("reasonsContent", "<ol><li>Reason 1</li></ol>"))
                                .andExpect(status().is3xxRedirection());

                String savedTitle = landingPageContentRepository.findByKey("hero_title")
                                .map(c -> c.getValue()).orElse(null);
                assertThat(savedTitle).isEqualTo("Persisted Hero Title");

                String savedTagline = landingPageContentRepository.findByKey("hero_tagline")
                                .map(c -> c.getValue()).orElse(null);
                assertThat(savedTagline).isEqualTo("Persisted tagline text");
        }

        @Test
        @WithMockUser(username = "tarparakrimy1@gmail.com", roles = "ADMIN")
        void landingPageEditor_AfterSave_ShouldDisplayUpdatedContent() throws Exception {
                mockMvc.perform(post("/admin/landing-page/save")
                                .with(csrf())
                                .param("heroTitle", "Updated Hero Title")
                                .param("heroTagline", "Updated Tagline")
                                .param("benefitsTitle", "Updated Benefits Title")
                                .param("reasonsHeading", "Updated Reasons Heading")
                                .param("reasonsContent", "<ol><li>Updated Reason</li></ol>"))
                                .andExpect(status().is3xxRedirection());

                mockMvc.perform(get("/admin/landing-page"))
                                .andExpect(status().isOk())
                                .andExpect(model().attribute("heroTitle", "Updated Hero Title"))
                                .andExpect(model().attribute("heroTagline", "Updated Tagline"))
                                .andExpect(model().attribute("benefitsTitle", "Updated Benefits Title"))
                                .andExpect(model().attribute("reasonsHeading", "Updated Reasons Heading"));
        }

        @Test
        @WithMockUser(username = "tarparakrimy1@gmail.com", roles = "ADMIN")
        void saveLandingPage_OverwriteExistingContent_ShouldUpdateSuccessfully() throws Exception {
                // Save initial content
                mockMvc.perform(post("/admin/landing-page/save")
                                .with(csrf())
                                .param("heroTitle", "Initial Title")
                                .param("heroTagline", "Initial Tagline")
                                .param("benefitsTitle", "Initial Benefits")
                                .param("reasonsHeading", "Initial Reasons")
                                .param("reasonsContent", "<ol><li>Initial</li></ol>"))
                                .andExpect(status().is3xxRedirection());

                // Overwrite with new content
                mockMvc.perform(post("/admin/landing-page/save")
                                .with(csrf())
                                .param("heroTitle", "Overwritten Title")
                                .param("heroTagline", "Overwritten Tagline")
                                .param("benefitsTitle", "Overwritten Benefits")
                                .param("reasonsHeading", "Overwritten Reasons")
                                .param("reasonsContent", "<ol><li>Overwritten</li></ol>"))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(flash().attributeExists("success"));

                String latestTitle = landingPageContentRepository.findByKey("hero_title")
                                .map(c -> c.getValue()).orElse(null);
                assertThat(latestTitle).isEqualTo("Overwritten Title");
        }

        @Test
        void landingPageEditor_WithoutAuthentication_ShouldRedirectToLogin() throws Exception {
                mockMvc.perform(get("/admin/landing-page"))
                                .andExpect(status().is3xxRedirection());
        }

        @Test
        @WithMockUser(username = "user@example.com", roles = "USER")
        void landingPageEditor_WithUserRole_ShouldBeForbidden() throws Exception {
                mockMvc.perform(get("/admin/landing-page"))
                                .andExpect(status().isForbidden());
        }

        @Test
        void saveLandingPage_WithoutAuthentication_ShouldRedirectToLogin() throws Exception {
                mockMvc.perform(post("/admin/landing-page/save")
                                .with(csrf())
                                .param("heroTitle", "Test Title")
                                .param("heroTagline", "Test Tagline")
                                .param("benefitsTitle", "Test Benefits")
                                .param("reasonsHeading", "Test Heading")
                                .param("reasonsContent", "<ol><li>Test</li></ol>"))
                                .andExpect(status().is3xxRedirection());
        }

        @Test
        @WithMockUser(username = "user@example.com", roles = "USER")
        void saveLandingPage_WithUserRole_ShouldBeForbidden() throws Exception {
                mockMvc.perform(post("/admin/landing-page/save")
                                .with(csrf())
                                .param("heroTitle", "Test Title")
                                .param("heroTagline", "Test Tagline")
                                .param("benefitsTitle", "Test Benefits")
                                .param("reasonsHeading", "Test Heading")
                                .param("reasonsContent", "<ol><li>Test</li></ol>"))
                                .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "tarparakrimy1@gmail.com", roles = "ADMIN")
        void saveLandingPage_WithoutCsrf_ShouldBeForbidden() throws Exception {
                mockMvc.perform(post("/admin/landing-page/save")
                                .param("heroTitle", "Test Title")
                                .param("heroTagline", "Test Tagline")
                                .param("benefitsTitle", "Test Benefits")
                                .param("reasonsHeading", "Test Heading")
                                .param("reasonsContent", "<ol><li>Test</li></ol>"))
                                .andExpect(status().isForbidden());
        }
}