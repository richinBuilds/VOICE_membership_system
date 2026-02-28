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
}
