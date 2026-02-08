package org.voice.membership.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.voice.membership.entities.Membership;
import org.voice.membership.entities.User;
import org.voice.membership.repositories.MembershipRepository;
import org.voice.membership.repositories.UserRepository;

import java.math.BigDecimal;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for membership cancellation workflow
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Membership Cancellation Integration Tests")
class MembershipCancellationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MembershipRepository membershipRepository;

    private User testUser;
    private Membership paidMembership;
    private Membership freeMembership;

    @BeforeEach
    void setUp() {
        // Clean up existing data
        userRepository.deleteAll();
        membershipRepository.deleteAll();

        // Create free membership
        freeMembership = Membership.builder()
                .name("Free Membership")
                .description("Basic access")
                .price(BigDecimal.ZERO)
                .isFree(true)
                .active(true)
                .displayOrder(1)
                .build();
        freeMembership = membershipRepository.save(freeMembership);

        // Create paid membership
        paidMembership = Membership.builder()
                .name("Premium Membership")
                .description("Full access")
                .price(new BigDecimal("20.00"))
                .isFree(false)
                .active(true)
                .displayOrder(2)
                .build();
        paidMembership = membershipRepository.save(paidMembership);

        // Create test user with paid membership
        testUser = User.builder()
                .email("cancellation.test@example.com")
                .password("$2a$10$dummypasswordhash")
                .firstName("Cancel")
                .lastName("Test")
                .role("USER")
                .emailVerified(true)
                .membership(paidMembership)
                .membershipStartDate(new Date())
                .creation(new Date())
                .build();
        testUser = userRepository.save(testUser);
    }

    @Test
    @DisplayName("Should display cancellation page for authenticated user with membership")
    @WithMockUser(username = "cancellation.test@example.com")
    void testCancellationPageAccessible() throws Exception {
        mockMvc.perform(get("/profile/cancel-membership"))
                .andExpect(status().isOk())
                .andExpect(view().name("cancel-membership"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attributeExists("userName"))
                .andExpect(model().attributeExists("currentMembershipName"))
                .andExpect(model().attribute("currentMembershipName", "Premium Membership"));
    }

    @Test
    @DisplayName("Should redirect to login when unauthenticated user tries to cancel")
    void testCancellationPageRedirectsWhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/profile/cancel-membership"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("Should successfully cancel paid membership")
    @WithMockUser(username = "cancellation.test@example.com")
    void testSuccessfulMembershipCancellation() throws Exception {
        mockMvc.perform(post("/profile/cancel-membership")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile?cancelled=true"));

        // Verify membership was changed to free
        User updatedUser = userRepository.findByEmail("cancellation.test@example.com");
        assertNotNull(updatedUser);
        assertNotNull(updatedUser.getMembership());
        assertTrue(updatedUser.getMembership().isFree());
        assertEquals("Free Membership", updatedUser.getMembership().getName());
    }

    @Test
    @DisplayName("Should redirect with error when user has no membership to cancel")
    @WithMockUser(username = "no.membership@example.com")
    void testCancellationFailsWhenNoMembership() throws Exception {
        // Create user without membership
        User userWithoutMembership = User.builder()
                .email("no.membership@example.com")
                .password("$2a$10$dummypasswordhash")
                .firstName("No")
                .lastName("Membership")
                .role("USER")
                .emailVerified(true)
                .creation(new Date())
                .build();
        userRepository.save(userWithoutMembership);

        mockMvc.perform(get("/profile/cancel-membership"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile?error=no_membership_to_cancel"));
    }

    @Test
    @DisplayName("Should not allow cancellation of free membership")
    @WithMockUser(username = "free.user@example.com")
    void testCannotCancelFreeMembership() throws Exception {
        // Create user with free membership
        User freeUser = User.builder()
                .email("free.user@example.com")
                .password("$2a$10$dummypasswordhash")
                .firstName("Free")
                .lastName("User")
                .role("USER")
                .emailVerified(true)
                .membership(freeMembership)
                .membershipStartDate(new Date())
                .creation(new Date())
                .build();
        userRepository.save(freeUser);

        // Should redirect with error when trying to access cancellation page
        mockMvc.perform(get("/profile/cancel-membership"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile?error=no_membership_to_cancel"));

        // Attempting to post should also redirect with error
        mockMvc.perform(post("/profile/cancel-membership")
                .with(csrf()))
                .andExpect(status().is3xxRedirection());

        // Verify membership is still free and unchanged
        User updatedUser = userRepository.findByEmail("free.user@example.com");
        assertNotNull(updatedUser);
        assertNotNull(updatedUser.getMembership());
        assertTrue(updatedUser.getMembership().isFree());
        assertEquals("Free Membership", updatedUser.getMembership().getName());
    }

    @Test
    @DisplayName("Should display profile with cancellation success message")
    @WithMockUser(username = "cancellation.test@example.com")
    void testProfileDisplaysCancellationSuccessMessage() throws Exception {
        mockMvc.perform(get("/profile").param("cancelled", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("profile"))
                .andExpect(model().attributeExists("user"));
    }

    @Test
    @DisplayName("Should not allow cancellation via GET request")
    @WithMockUser(username = "cancellation.test@example.com")
    void testCancellationOnlyAllowsPostMethod() throws Exception {
        mockMvc.perform(get("/profile/cancel-membership")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("cancel-membership"));

        // Verify membership was NOT cancelled
        User unchangedUser = userRepository.findByEmail("cancellation.test@example.com");
        assertNotNull(unchangedUser);
        assertNotNull(unchangedUser.getMembership());
        assertFalse(unchangedUser.getMembership().isFree());
        assertEquals("Premium Membership", unchangedUser.getMembership().getName());
    }

    @Test
    @DisplayName("Should require CSRF token for cancellation POST")
    @WithMockUser(username = "cancellation.test@example.com")
    void testCancellationRequiresCsrfToken() throws Exception {
        mockMvc.perform(post("/profile/cancel-membership"))
                .andExpect(status().isForbidden());

        // Verify membership was NOT cancelled
        User unchangedUser = userRepository.findByEmail("cancellation.test@example.com");
        assertNotNull(unchangedUser);
        assertFalse(unchangedUser.getMembership().isFree());
    }

    @Test
    @DisplayName("Should show cancel button on profile page when user has membership")
    @WithMockUser(username = "cancellation.test@example.com")
    void testProfileShowsCancelButton() throws Exception {
        mockMvc.perform(get("/profile"))
                .andExpect(status().isOk())
                .andExpect(view().name("profile"))
                .andExpect(model().attribute("membershipType", "Premium Membership"));
    }
}
