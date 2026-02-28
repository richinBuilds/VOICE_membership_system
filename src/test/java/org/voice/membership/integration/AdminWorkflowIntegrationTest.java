package org.voice.membership.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.voice.membership.entities.Membership;
import org.voice.membership.entities.Role;
import org.voice.membership.entities.User;
import org.voice.membership.repositories.ChildRepository;
import org.voice.membership.repositories.MembershipRepository;
import org.voice.membership.repositories.UserRepository;

import java.math.BigDecimal;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for Admin Dashboard workflows.
 * Tests the ACTUAL AdminController endpoints and functionality:
 * - /admin/dashboard (view: "admin") with filtering
 * - /admin/user/{id} (REST JSON endpoint)
 * - /admin/export-users (Excel export)
 * - /admin/add-member (view: "admin-add-member")
 * - /admin/edit-member/{id} (view: "admin-edit-member")
 * - /admin/delete-member/{id} (redirect)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminWorkflowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChildRepository childRepository;

    @Autowired
    private MembershipRepository membershipRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User adminUser;
    private User testMember1;
    private User testMember2;
    private Membership freeMembership;
    private Membership paidMembership;

    @BeforeEach
    void setUp() {
        childRepository.deleteAll();
        userRepository.deleteAll();
        membershipRepository.deleteAll();

        // Create memberships
        freeMembership = new Membership();
        freeMembership.setName("Free");
        freeMembership.setPrice(BigDecimal.ZERO);
        freeMembership.setActive(true);
        freeMembership.setFree(true);
        freeMembership.setDisplayOrder(1);
        freeMembership = membershipRepository.save(freeMembership);

        paidMembership = new Membership();
        paidMembership.setName("Premium");
        paidMembership.setPrice(new BigDecimal("50.00"));
        paidMembership.setActive(true);
        paidMembership.setFree(false);
        paidMembership.setDisplayOrder(2);
        paidMembership = membershipRepository.save(paidMembership);

        // Create admin user
        adminUser = User.builder()
                .firstName("Admin")
                .lastName("User")
                .email("admin@example.com")
                .password(passwordEncoder.encode("Admin123!"))
                .role(Role.ADMIN.name())
                .emailVerified(true)
                .creation(new Date())
                .build();
        adminUser = userRepository.save(adminUser);

        // Create test members
        testMember1 = User.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .password(passwordEncoder.encode("Password123!"))
                .phone("4161234567")
                .address("123 Main St")
                .city("Toronto")
                .province("ON")
                .postalCode("M1M1M1")
                .role(Role.USER.name())
                .emailVerified(true)
                .membership(freeMembership)
                .creation(new Date())
                .build();
        testMember1 = userRepository.save(testMember1);

        testMember2 = User.builder()
                .firstName("Jane")
                .lastName("Smith")
                .email("jane.smith@example.com")
                .password(passwordEncoder.encode("Password123!"))
                .phone("4169876543")
                .address("456 Oak Ave")
                .city("Ottawa")
                .province("ON")
                .postalCode("K1K1K1")
                .role(Role.USER.name())
                .emailVerified(true)
                .membership(paidMembership)
                .creation(new Date())
                .build();
        testMember2 = userRepository.save(testMember2);
    }

    // ==================== Access Control Tests ====================

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void testAdminCanAccessDashboard() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin"))
                .andExpect(model().attributeExists("users"))
                .andExpect(model().attributeExists("totalUsers"))
                .andExpect(model().attributeExists("adminName"))
                .andExpect(model().attributeExists("adminEmail"));
    }

    @Test
    @WithMockUser(username = "john.doe@example.com", roles = "USER")
    void testNonAdminCannotAccessDashboard() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isForbidden());
    }

    // ==================== Search & Filtering Tests ====================

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void testAdminDashboardWithCityFilter() throws Exception {
        mockMvc.perform(get("/admin/dashboard")
                .param("city", "Toronto"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin"))
                .andExpect(model().attributeExists("users"))
                .andExpect(model().attribute("city", "Toronto"));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void testAdminDashboardWithProvinceFilter() throws Exception {
        mockMvc.perform(get("/admin/dashboard")
                .param("province", "ON"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin"))
                .andExpect(model().attributeExists("users"))
                .andExpect(model().attribute("province", "ON"));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void testAdminDashboardWithAddressFilter() throws Exception {
        mockMvc.perform(get("/admin/dashboard")
                .param("address", "123 Main St"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin"))
                .andExpect(model().attributeExists("users"))
                .andExpect(model().attribute("address", "123 Main St"));
    }

    // ==================== Member Management Tests ====================

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void testShowAddMemberForm() throws Exception {
        mockMvc.perform(get("/admin/add-member"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-add-member"))
                .andExpect(model().attributeExists("memberRequest"))
                .andExpect(model().attributeExists("memberships"))
                .andExpect(model().attributeExists("adminName"))
                .andExpect(model().attributeExists("adminEmail"));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void testCreateMemberSuccessfully() throws Exception {
        mockMvc.perform(post("/admin/add-member")
                .with(csrf())
                .param("firstName", "New")
                .param("lastName", "Member")
                .param("email", "new.member@example.com")
                .param("password", "Password123!")
                .param("confirmPassword", "Password123!")
                .param("phone", "4165551234")
                .param("address", "789 New St")
                .param("city", "Toronto")
                .param("province", "ON")
                .param("postalCode", "M2M2M2")
                .param("membershipId", String.valueOf(freeMembership.getId()))
                .param("emailVerified", "true")
                .param("accountLocked", "false"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"))
                .andExpect(flash().attributeExists("success"));

        // Verify member was created
        User created = userRepository.findByEmail("new.member@example.com");
        assertThat(created).isNotNull();
        assertThat(created.getFirstName()).isEqualTo("New");
        assertThat(created.getLastName()).isEqualTo("Member");
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void testCreateMemberWithDuplicateEmail() throws Exception {
        mockMvc.perform(post("/admin/add-member")
                .with(csrf())
                .param("firstName", "Duplicate")
                .param("lastName", "User")
                .param("email", "john.doe@example.com") // Already exists
                .param("password", "Password123!")
                .param("confirmPassword", "Password123!")
                .param("phone", "4165551234")
                .param("address", "789 New St")
                .param("city", "Toronto")
                .param("province", "ON")
                .param("postalCode", "M2M2M2")
                .param("membershipId", String.valueOf(freeMembership.getId()))
                .param("emailVerified", "true")
                .param("accountLocked", "false"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/add-member"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void testCreateMemberWithPasswordMismatch() throws Exception {
        mockMvc.perform(post("/admin/add-member")
                .with(csrf())
                .param("firstName", "New")
                .param("lastName", "Member")
                .param("email", "another.member@example.com")
                .param("password", "Password123!")
                .param("confirmPassword", "DifferentPassword123!")
                .param("phone", "4165551234")
                .param("address", "789 New St")
                .param("city", "Toronto")
                .param("province", "ON")
                .param("postalCode", "M2M2M2")
                .param("membershipId", String.valueOf(freeMembership.getId()))
                .param("emailVerified", "true")
                .param("accountLocked", "false"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/add-member"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void testShowEditMemberForm() throws Exception {
        mockMvc.perform(get("/admin/edit-member/" + testMember1.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-edit-member"))
                .andExpect(model().attributeExists("updateRequest"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attributeExists("memberships"))
                .andExpect(model().attributeExists("adminName"))
                .andExpect(model().attributeExists("adminEmail"));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void testEditMemberSuccessfully() throws Exception {
        mockMvc.perform(post("/admin/edit-member/" + testMember1.getId())
                .with(csrf())
                .param("userId", String.valueOf(testMember1.getId()))
                .param("firstName", "John Updated")
                .param("lastName", "Doe Updated")
                .param("email", "john.updated@example.com")
                .param("phone", "4161111111")
                .param("address", "123 Updated St")
                .param("city", "Toronto")
                .param("province", "ON")
                .param("postalCode", "M1M1M1")
                .param("emailVerified", "true")
                .param("accountLocked", "false"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"))
                .andExpect(flash().attributeExists("success"));

        // Verify member was updated
        User updated = userRepository.findById(testMember1.getId()).orElse(null);
        assertThat(updated).isNotNull();
        assertThat(updated.getFirstName()).isEqualTo("John Updated");
        assertThat(updated.getEmail()).isEqualTo("john.updated@example.com");
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void testEditMemberWithDuplicateEmail() throws Exception {
        mockMvc.perform(post("/admin/edit-member/" + testMember1.getId())
                .with(csrf())
                .param("userId", String.valueOf(testMember1.getId()))
                .param("firstName", "John")
                .param("lastName", "Doe")
                .param("email", "jane.smith@example.com") // Email of testMember2
                .param("phone", "4161234567")
                .param("address", "123 Main St")
                .param("city", "Toronto")
                .param("province", "ON")
                .param("postalCode", "M1M1M1")
                .param("emailVerified", "true")
                .param("accountLocked", "false"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-edit-member"))
                .andExpect(model().attributeHasErrors("updateRequest"));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void testEditNonExistentMember() throws Exception {
        mockMvc.perform(get("/admin/edit-member/99999"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void testDeleteMemberSuccessfully() throws Exception {
        mockMvc.perform(post("/admin/delete-member/" + testMember1.getId())
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"))
                .andExpect(flash().attributeExists("success"));

        // Verify member was deleted
        User deleted = userRepository.findById(testMember1.getId()).orElse(null);
        assertThat(deleted).isNull();
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void testDeleteNonExistentMember() throws Exception {
        mockMvc.perform(post("/admin/delete-member/99999")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"))
                .andExpect(flash().attributeExists("error"));
    }

    // ==================== REST Endpoint Tests ====================

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void testGetUserDetailsViaRestEndpoint() throws Exception {
        mockMvc.perform(get("/admin/user/" + testMember1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testMember1.getId()))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.email").value("john.doe@example.com"));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void testGetNonExistentUserDetails() throws Exception {
        mockMvc.perform(get("/admin/user/99999"))
                .andExpect(status().isNotFound());
    }

    // ==================== Export Tests ====================

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void testExportUsersToExcel() throws Exception {
        mockMvc.perform(get("/admin/export-users"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().string("Content-Disposition",
                        containsString("attachment; filename=users_and_children_")));
    }
}
