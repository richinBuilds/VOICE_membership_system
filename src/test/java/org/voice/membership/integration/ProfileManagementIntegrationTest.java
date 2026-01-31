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
import org.voice.membership.entities.Role;
import org.voice.membership.entities.User;
import org.voice.membership.repositories.ChildRepository;
import org.voice.membership.repositories.UserRepository;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end integration tests for Profile Management
 * Tests complete profile and child management workflows
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ProfileManagementIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private ChildRepository childRepository;

        private User testUser;

        @BeforeEach
        void setUp() {
                childRepository.deleteAll();
                userRepository.deleteAll();

                testUser = User.builder()
                                .name("Integration Test User")
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

        // Test: View profile, navigate to edit page, update profile data, and verify
        // changes
        @Test
        @WithMockUser(username = "integration@example.com", roles = "USER")
        void testCompleteProfileViewAndEdit() throws Exception {
                // Step 1: View profile
                mockMvc.perform(get("/profile"))
                                .andExpect(status().isOk())
                                .andExpect(view().name("profile"))
                                .andExpect(model().attribute("userName", "Integration Test User"));

                // Step 2: Navigate to edit
                mockMvc.perform(get("/profile/edit"))
                                .andExpect(status().isOk())
                                .andExpect(view().name("editProfile"));

                // Step 3: Update profile with valid data
                mockMvc.perform(post("/profile/edit")
                                .with(csrf())
                                .param("name", "Updated Test User")
                                .param("email", "integration@example.com")
                                .param("phone", "9876543210")
                                .param("address", "456 Updated St")
                                .param("postalCode", "M5H 2N2"))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/profile"));

                // Verify update
                User updated = userRepository.findByEmail("integration@example.com");
                assertThat(updated.getName()).isEqualTo("Updated Test User");
                assertThat(updated.getPhone()).isEqualTo("9876543210");
        }

        // Test: Add a child, verify it was saved, delete the child, and verify deletion
        @Test
        @WithMockUser(username = "integration@example.com", roles = "USER")
        void testCompleteChildManagementWorkflow() throws Exception {
                // Step 1: Add child page
                mockMvc.perform(get("/profile/child/add"))
                                .andExpect(status().isOk())
                                .andExpect(view().name("editChild"));

                // Step 2: Submit new child
                mockMvc.perform(post("/profile/child/add")
                                .with(csrf())
                                .param("name", "Test Child")
                                .param("dateOfBirth", "2020-01-01")
                                .param("hearingLossType", "Profound")
                                .param("equipmentType", "Cochlear Implant"))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/profile"));

                // Verify child was added
                User user = userRepository.findByEmail("integration@example.com");
                List<Child> children = childRepository.findByUser(user);
                assertThat(children).isNotNull();
                assertThat(children).hasSize(1);
                assertThat(children.get(0).getName()).isEqualTo("Test Child");

                // Step 3: Delete child
                Child child = children.get(0);
                mockMvc.perform(post("/profile/child/delete/" + child.getId())
                                .with(csrf()))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/profile"));

                // Verify child was deleted
                List<Child> childrenAfterDelete = childRepository.findByUser(user);
                assertThat(childrenAfterDelete).isEmpty();
        }

        // Test: Verify logged-in user can access profile and see user/children data
        @Test
        @WithMockUser(username = "integration@example.com", roles = "USER")
        void testProfileAccessWithAuthentication() throws Exception {
                mockMvc.perform(get("/profile"))
                                .andExpect(status().isOk())
                                .andExpect(model().attributeExists("user"))
                                .andExpect(model().attributeExists("children"));
        }

        // Test: Verify unauthenticated user is redirected when trying to access profile
        @Test
        void testProfileAccessWithoutAuthenticationRedirects() throws Exception {
                mockMvc.perform(get("/profile"))
                                .andExpect(status().is3xxRedirection());
        }
}
