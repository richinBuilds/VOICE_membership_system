package org.voice.membership.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.voice.membership.entities.Child;
import org.voice.membership.entities.Membership;
import org.voice.membership.entities.Role;
import org.voice.membership.entities.User;
import org.voice.membership.repositories.ChildRepository;
import org.voice.membership.repositories.MembershipRepository;
import org.voice.membership.repositories.UserRepository;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end workflow tests for Profile Management
 * Tests complete user journeys: profile editing, child management workflows
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ProfileWorkflowTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private ChildRepository childRepository;

        @Autowired
        private MembershipRepository membershipRepository;

        private User testUser;

        @BeforeEach
        void setUp() {
                childRepository.deleteAll();
                userRepository.deleteAll();

                testUser = User.builder()
                                .firstName("Integration")
                                .middleName("Test")
                                .lastName("User")
                                .email("integration@example.com")
                                .password("$2a$10$encodedPassword")
                                .phone("1234567890")
                                .address("123 Test St")
                                .postalCode("A1A 1A1")
                                .role(Role.USER.name())
                                .creation(new Date())
                                .build();

                testUser = userRepository.save(testUser);
        }

        // changes
        @Test
        @WithMockUser(username = "integration@example.com", roles = "USER")
        void testCompleteProfileViewAndEdit() throws Exception {
                mockMvc.perform(get("/profile"))
                                .andExpect(status().isOk())
                                .andExpect(view().name("profile"))
                                .andExpect(model().attribute("userName", "Integration Test User"));

                mockMvc.perform(get("/profile/edit"))
                                .andExpect(status().isOk())
                                .andExpect(view().name("editProfile"));

                mockMvc.perform(post("/profile/edit")
                                .with(csrf())
                                .param("firstName", "Updated")
                                .param("middleName", "Test")
                                .param("lastName", "User")
                                .param("email", "integration@example.com")
                                .param("phone", "9876543210")
                                .param("address", "456 Updated St")
                                .param("city", "Calgary")
                                .param("province", "AB")
                                .param("postalCode", "M5H 2N2"))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/profile"));

                User updated = userRepository.findByEmail("integration@example.com");
                assertThat(updated.getFirstName()).isEqualTo("Updated");
                assertThat(updated.getMiddleName()).isEqualTo("Test");
                assertThat(updated.getLastName()).isEqualTo("User");
                assertThat(updated.getPhone()).isEqualTo("9876543210");
        }

        @Test
        @WithMockUser(username = "integration@example.com", roles = "USER")
        void testCompleteChildManagementWorkflow() throws Exception {
                mockMvc.perform(get("/profile/child/add"))
                                .andExpect(status().isOk())
                                .andExpect(view().name("editChild"));

                mockMvc.perform(post("/profile/child/add")
                                .with(csrf())
                                .param("name", "Test Child")
                                .param("dateOfBirth", "2020-01-01")
                                .param("hearingLossType", "Profound")
                                .param("equipmentType", "Cochlear Implant"))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/profile"));

                User user = userRepository.findByEmail("integration@example.com");
                List<Child> children = childRepository.findByUser(user);
                assertThat(children).isNotNull();
                assertThat(children).hasSize(1);
                assertThat(children.get(0).getName()).isEqualTo("Test Child");

                Child child = children.get(0);
                mockMvc.perform(post("/profile/child/delete/" + child.getId())
                                .with(csrf()))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/profile"));

                List<Child> childrenAfterDelete = childRepository.findByUser(user);
                assertThat(childrenAfterDelete).isEmpty();
        }

        @Test
        @WithMockUser(username = "integration@example.com", roles = "USER")
        void testProfileAccessWithAuthentication() throws Exception {
                mockMvc.perform(get("/profile"))
                                .andExpect(status().isOk())
                                .andExpect(model().attributeExists("user"))
                                .andExpect(model().attributeExists("children"));
        }

        @Test
        void testProfileAccessWithoutAuthenticationRedirects() throws Exception {
                mockMvc.perform(get("/profile"))
                                .andExpect(status().is3xxRedirection());
        }

        // upgrades to paid
        @Test
        @WithMockUser(username = "integration@example.com", roles = "USER")
        void testCompleteUpgradeMembershipWorkflow() throws Exception {
                Membership freeMembership = new Membership();
                freeMembership.setName("Free Membership");
                freeMembership.setFree(true);
                freeMembership.setPrice(BigDecimal.ZERO);
                freeMembership = membershipRepository.save(freeMembership);

                testUser.setMembership(freeMembership);
                userRepository.save(testUser);

                Membership paidMembership = new Membership();
                paidMembership.setName("Premium Membership");
                paidMembership.setFree(false);
                paidMembership.setPrice(new BigDecimal("50.00"));
                paidMembership = membershipRepository.save(paidMembership);

                mockMvc.perform(get("/profile"))
                                .andExpect(status().isOk())
                                .andExpect(view().name("profile"))
                                .andExpect(model().attributeExists("user"));

                mockMvc.perform(get("/profile/upgrade-membership"))
                                .andExpect(status().isOk())
                                .andExpect(view().name("upgrade-membership"))
                                .andExpect(model().attributeExists("paidMemberships"));

                mockMvc.perform(post("/profile/upgrade-membership/select")
                                .with(csrf())
                                .param("membershipId", String.valueOf(paidMembership.getId())))
                                .andExpect(status().isOk())
                                .andExpect(view().name("upgrade-checkout"))
                                .andExpect(model().attributeExists("upgradeMembership"));

                User updatedUser = userRepository.findByEmail("integration@example.com");
                assertThat(updatedUser.getMembership().getId()).isEqualTo(freeMembership.getId());
        }
}
