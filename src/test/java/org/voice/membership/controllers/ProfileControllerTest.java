package org.voice.membership.controllers;

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
import org.voice.membership.entities.Child;
import org.voice.membership.entities.User;
import org.voice.membership.repositories.ChildRepository;
import org.voice.membership.repositories.MembershipRepository;
import org.voice.membership.repositories.UserRepository;
import java.util.Date;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for ProfileController
 * Tests user profile management functionality using real services
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ProfileControllerTest {

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

    private User testUser;
    private Child testChild;

    @BeforeEach
    void setUp() {
        childRepository.deleteAll();
        userRepository.deleteAll();

        // Create real test user
        testUser = new User();
        testUser.setName("Test User");
        testUser.setEmail("test@example.com");
        testUser.setPassword(passwordEncoder.encode("password"));
        testUser.setPhone("1234567890");
        testUser.setAddress("123 Test St");
        testUser.setPostalCode("M5H 2N2");
        testUser.setRole("USER");
        testUser.setCreation(new Date());
        testUser = userRepository.save(testUser);

        // Create real test child
        testChild = new Child();
        testChild.setName("Child One");
        testChild.setDateOfBirth(new Date());
        testChild.setHearingLossType("Profound");
        testChild.setEquipmentType("Cochlear Implant");
        testChild.setUser(testUser);
        testChild = childRepository.save(testChild);
    }

    // ===============================Positive Tests==============================
    // View Profile Page
    @Test
    @WithMockUser(username = "test@example.com", roles = "USER")
    void profile_WithAuthenticatedUser_ShouldReturnProfilePage() throws Exception {
        mockMvc.perform(get("/profile"))
                .andExpect(status().isOk())
                .andExpect(view().name("profile"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attribute("userName", "Test User"))
                .andExpect(model().attribute("userEmail", "test@example.com"));
    }

    // Show Edit Profile Form
    @Test
    @WithMockUser(username = "test@example.com", roles = "USER")
    void editProfile_GetRequest_ShouldReturnEditForm() throws Exception {
        mockMvc.perform(get("/profile/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("editProfile"))
                .andExpect(model().attributeExists("updateUserRequest"));
    }

    // Submit Profile Changes
    @Test
    @WithMockUser(username = "test@example.com", roles = "USER")
    void editProfile_PostRequest_WithValidData_ShouldUpdateProfile() throws Exception {
        mockMvc.perform(post("/profile/edit")
                .with(csrf())
                .param("name", "Updated Name")
                .param("email", "test@example.com")
                .param("phone", "9876543210")
                .param("address", "456 New St")
                .param("postalCode", "A1A 1A1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"));
    }

    // Show Add Child Form
    @Test
    @WithMockUser(username = "test@example.com", roles = "USER")
    void addChild_GetRequest_ShouldReturnAddChildForm() throws Exception {
        mockMvc.perform(get("/profile/child/add"))
                .andExpect(status().isOk())
                .andExpect(view().name("editChild"))
                .andExpect(model().attributeExists("child"))
                .andExpect(model().attribute("isEdit", false));
    }

    // Delete a Child
    @Test
    @WithMockUser(username = "test@example.com", roles = "USER")
    void deleteChild_WithValidId_ShouldDeleteChild() throws Exception {
        mockMvc.perform(post("/profile/child/delete/" + testChild.getId())
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"));
    }

    // Profile Without Login
    @Test
    void profile_WithoutAuthentication_ShouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/profile"))
                .andExpect(status().is3xxRedirection());
    }

    // View upgrade membership page with free membership
    @Test
    @WithMockUser(username = "test@example.com", roles = "USER")
    void upgradeMembershipPage_WithFreeMembership_ShouldShowUpgradeOptions() throws Exception {
        User user = userRepository.findByEmail("test@example.com");

        org.voice.membership.entities.Membership freeMembership = new org.voice.membership.entities.Membership();
        freeMembership.setName("Free Membership");
        freeMembership.setPrice(java.math.BigDecimal.ZERO);
        freeMembership.setActive(true);
        freeMembership.setFree(true);
        freeMembership.setDisplayOrder(1);
        freeMembership = membershipRepository.save(freeMembership);

        user.setMembership(freeMembership);
        userRepository.save(user);

        org.voice.membership.entities.Membership paidMembership = new org.voice.membership.entities.Membership();
        paidMembership.setName("Premium Membership");
        paidMembership.setPrice(new java.math.BigDecimal("50.00"));
        paidMembership.setActive(true);
        paidMembership.setFree(false);
        paidMembership.setDisplayOrder(2);
        membershipRepository.save(paidMembership);

        mockMvc.perform(get("/profile/upgrade-membership"))
                .andExpect(status().isOk())
                .andExpect(view().name("upgrade-membership"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attributeExists("currentMembership"))
                .andExpect(model().attributeExists("paidMemberships"));
    }

    // Select upgrade membership with valid paid membership
    @Test
    @WithMockUser(username = "test@example.com", roles = "USER")
    void selectUpgradeMembership_WithValidMembership_ShouldShowCheckout() throws Exception {
        User user = userRepository.findByEmail("test@example.com");

        org.voice.membership.entities.Membership freeMembership = new org.voice.membership.entities.Membership();
        freeMembership.setName("Free Membership");
        freeMembership.setPrice(java.math.BigDecimal.ZERO);
        freeMembership.setActive(true);
        freeMembership.setFree(true);
        freeMembership.setDisplayOrder(1);
        freeMembership = membershipRepository.save(freeMembership);

        user.setMembership(freeMembership);
        userRepository.save(user);

        org.voice.membership.entities.Membership paidMembership = new org.voice.membership.entities.Membership();
        paidMembership.setName("Premium Membership");
        paidMembership.setPrice(new java.math.BigDecimal("50.00"));
        paidMembership.setActive(true);
        paidMembership.setFree(false);
        paidMembership.setDisplayOrder(2);
        paidMembership = membershipRepository.save(paidMembership);

        mockMvc.perform(post("/profile/upgrade-membership/select")
                .with(csrf())
                .param("membershipId", String.valueOf(paidMembership.getId())))
                .andExpect(status().isOk())
                .andExpect(view().name("upgrade-checkout"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attributeExists("upgradeMembership"))
                .andExpect(model().attributeExists("membershipName"))
                .andExpect(model().attributeExists("membershipPrice"));
    }

    // ==============================Negative Tests==============================
    // Delete Child That Doesn't Exist - Controller redirects with error message
    @Test
    @WithMockUser(username = "test@example.com", roles = "USER")
    void deleteChild_WithInvalidId_ShouldRedirectToProfile() throws Exception {
        mockMvc.perform(post("/profile/child/delete/99999")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"));
    }

    // Delete Someone Else's Child
    @Test
    @WithMockUser(username = "test@example.com", roles = "USER")
    void deleteChild_BelongingToAnotherUser_ShouldRedirectWithError() throws Exception {
        User otherUser = new User();
        otherUser.setName("Other User");
        otherUser.setEmail("other@example.com");
        otherUser.setPassword(passwordEncoder.encode("password"));
        otherUser.setPhone("9999999999");
        otherUser.setPostalCode("A1A 1A1");
        otherUser.setRole("USER");
        otherUser.setCreation(new Date());
        otherUser = userRepository.save(otherUser);

        Child otherChild = new Child();
        otherChild.setName("Other Child");
        otherChild.setDateOfBirth(new Date());
        otherChild.setHearingLossType("Moderate");
        otherChild.setEquipmentType("Hearing Aid");
        otherChild.setUser(otherUser);
        otherChild = childRepository.save(otherChild);

        mockMvc.perform(post("/profile/child/delete/" + otherChild.getId())
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"));
    }

    // Edit Profile With Invalid Postal Code
    @Test
    @WithMockUser(username = "test@example.com", roles = "USER")
    void editProfile_WithInvalidPostalCode_ShouldShowValidationError() throws Exception {
        mockMvc.perform(post("/profile/edit")
                .with(csrf())
                .param("name", "Updated Name")
                .param("email", "test@example.com")
                .param("phone", "9876543210")
                .param("postalCode", "12345"))
                .andExpect(status().isOk())
                .andExpect(view().name("editProfile"))
                .andExpect(model().hasErrors());
    }

    // Edit Profile With Missing Required Fields
    @Test
    @WithMockUser(username = "test@example.com", roles = "USER")
    void editProfile_WithMissingRequiredFields_ShouldShowValidationError() throws Exception {
        mockMvc.perform(post("/profile/edit")
                .with(csrf())
                .param("name", "")
                .param("email", "")
                .param("phone", ""))
                .andExpect(status().isOk())
                .andExpect(model().hasErrors());
    }

    // Edit Profile With Email That Already Exists
    @Test
    @WithMockUser(username = "test@example.com", roles = "USER")
    void editProfile_WithDuplicateEmail_ShouldShowValidationError() throws Exception {
        User otherUser = new User();
        otherUser.setName("Other User");
        otherUser.setEmail("other@example.com");
        otherUser.setPassword(passwordEncoder.encode("password"));
        otherUser.setPhone("9999999999");
        otherUser.setAddress("789 Other St");
        otherUser.setPostalCode("B2B 2B2");
        otherUser.setRole("USER");
        otherUser.setCreation(new Date());
        userRepository.save(otherUser);

        mockMvc.perform(post("/profile/edit")
                .with(csrf())
                .param("name", "Test User")
                .param("email", "other@example.com")
                .param("phone", "1234567890")
                .param("address", "123 Test St")
                .param("postalCode", "M5H 2N2"))
                .andExpect(status().isOk())
                .andExpect(view().name("editProfile"))
                .andExpect(model().hasErrors());
    }

    // Operations Without CSRF Token
    @Test
    @WithMockUser(username = "test@example.com", roles = "USER")
    void deleteChild_WithoutCSRF_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(post("/profile/child/delete/" + testChild.getId()))
                .andExpect(status().isForbidden());
    }

    // View upgrade membership page with paid membership
    @Test
    @WithMockUser(username = "test@example.com", roles = "USER")
    void upgradeMembershipPage_WithPaidMembership_ShouldRedirectToProfile() throws Exception {
        User user = userRepository.findByEmail("test@example.com");

        org.voice.membership.entities.Membership paidMembership = new org.voice.membership.entities.Membership();
        paidMembership.setName("Premium Membership");
        paidMembership.setPrice(new java.math.BigDecimal("50.00"));
        paidMembership.setActive(true);
        paidMembership.setFree(false);
        paidMembership.setDisplayOrder(2);
        paidMembership = membershipRepository.save(paidMembership);

        user.setMembership(paidMembership);
        userRepository.save(user);

        mockMvc.perform(get("/profile/upgrade-membership"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile?error=not_eligible_for_upgrade"));
    }

    // View upgrade membership page without membership
    @Test
    @WithMockUser(username = "test@example.com", roles = "USER")
    void upgradeMembershipPage_WithoutMembership_ShouldRedirectToProfile() throws Exception {
        User user = userRepository.findByEmail("test@example.com");
        user.setMembership(null);
        userRepository.save(user);
        mockMvc.perform(get("/profile/upgrade-membership"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile?error=not_eligible_for_upgrade"));
    }

    // View upgrade membership page with paid membership
    @Test
    @WithMockUser(username = "test@example.com", roles = "USER")
    void selectUpgradeMembership_WithPaidMembership_ShouldRedirectToProfile() throws Exception {
        User user = userRepository.findByEmail("test@example.com");

        org.voice.membership.entities.Membership existingPaidMembership = new org.voice.membership.entities.Membership();
        existingPaidMembership.setName("Existing Premium");
        existingPaidMembership.setPrice(new java.math.BigDecimal("40.00"));
        existingPaidMembership.setActive(true);
        existingPaidMembership.setFree(false);
        existingPaidMembership.setDisplayOrder(2);
        existingPaidMembership = membershipRepository.save(existingPaidMembership);

        user.setMembership(existingPaidMembership);
        userRepository.save(user);

        org.voice.membership.entities.Membership paidMembership = new org.voice.membership.entities.Membership();
        paidMembership.setName("Premium Membership");
        paidMembership.setPrice(new java.math.BigDecimal("50.00"));
        paidMembership.setActive(true);
        paidMembership.setFree(false);
        paidMembership.setDisplayOrder(3);
        paidMembership = membershipRepository.save(paidMembership);

        mockMvc.perform(post("/profile/upgrade-membership/select")
                .with(csrf())
                .param("membershipId", String.valueOf(paidMembership.getId())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile?error=not_eligible_for_upgrade"));
    }

    // View upgrade membership page with invalid membership ID
    @Test
    @WithMockUser(username = "test@example.com", roles = "USER")
    void selectUpgradeMembership_WithInvalidMembershipId_ShouldRedirectWithError() throws Exception {
        User user = userRepository.findByEmail("test@example.com");

        org.voice.membership.entities.Membership freeMembership = new org.voice.membership.entities.Membership();
        freeMembership.setName("Free Membership");
        freeMembership.setPrice(java.math.BigDecimal.ZERO);
        freeMembership.setActive(true);
        freeMembership.setFree(true);
        freeMembership.setDisplayOrder(1);
        freeMembership = membershipRepository.save(freeMembership);

        user.setMembership(freeMembership);
        userRepository.save(user);

        mockMvc.perform(post("/profile/upgrade-membership/select")
                .with(csrf())
                .param("membershipId", "99999"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile/upgrade-membership?error=invalid_membership"));
    }

    // View upgrade membership page with free membership
    @Test
    @WithMockUser(username = "test@example.com", roles = "USER")
    void selectUpgradeMembership_WithFreeMembershipId_ShouldRedirectWithError() throws Exception {
        User user = userRepository.findByEmail("test@example.com");

        org.voice.membership.entities.Membership freeMembership = new org.voice.membership.entities.Membership();
        freeMembership.setName("Free Membership");
        freeMembership.setPrice(java.math.BigDecimal.ZERO);
        freeMembership.setActive(true);
        freeMembership.setFree(true);
        freeMembership.setDisplayOrder(1);
        freeMembership = membershipRepository.save(freeMembership);

        user.setMembership(freeMembership);
        userRepository.save(user);

        org.voice.membership.entities.Membership anotherFreeMembership = new org.voice.membership.entities.Membership();
        anotherFreeMembership.setName("Another Free Membership");
        anotherFreeMembership.setPrice(java.math.BigDecimal.ZERO);
        anotherFreeMembership.setActive(true);
        anotherFreeMembership.setFree(true);
        anotherFreeMembership.setDisplayOrder(2);
        anotherFreeMembership = membershipRepository.save(anotherFreeMembership);

        mockMvc.perform(post("/profile/upgrade-membership/select")
                .with(csrf())
                .param("membershipId", String.valueOf(anotherFreeMembership.getId())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile/upgrade-membership?error=invalid_membership"));
    }

}
