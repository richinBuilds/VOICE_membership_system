package org.voice.membership.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.voice.membership.entities.Membership;
import org.voice.membership.repositories.MembershipRepository;
import org.voice.membership.repositories.UserRepository;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Use case: Edit Membership Plan Details")
class EditMembershipPlanDetailsTest {

    private static final String ADMIN_EMAIL = "tarparakrimy1@gmail.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MembershipRepository membershipRepository;

    private Membership freeMembership;
    private Membership premiumMembership;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        membershipRepository.deleteAll();

        freeMembership = Membership.builder()
                .name("Free")
                .description("Free membership")
                .price(new BigDecimal("0.00"))
                .features("Basic features")
                .isFree(true)
                .displayOrder(1)
                .active(true)
                .build();
        freeMembership = membershipRepository.save(freeMembership);

        premiumMembership = Membership.builder()
                .name("Premium")
                .description("Premium membership with all features")
                .price(new BigDecimal("20.00"))
                .features("- All features\n- Priority support")
                .isFree(false)
                .displayOrder(2)
                .active(true)
                .build();
        premiumMembership = membershipRepository.save(premiumMembership);
    }

    @Nested
    @DisplayName("When admin is logged in and plans exist (preconditions satisfied)")
    class WhenAdminAndPlansExist {

        @Test
        @WithMockUser(username = ADMIN_EMAIL, roles = "ADMIN")
        @DisplayName("Trigger: dashboard links to membership plan management")
        void adminDashboard_exposesLinkToMembershipManagement() throws Exception {
            mockMvc.perform(get("/admin/dashboard"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("/admin/memberships")));
        }

        @Test
        @WithMockUser(username = ADMIN_EMAIL, roles = "ADMIN")
        @DisplayName("Membership section shows existing plans ready to edit")
        void openMembershipManagement_showsAllActivePlans() throws Exception {
            mockMvc.perform(get("/admin/memberships"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin-edit-memberships"))
                    .andExpect(model().attributeExists("memberships"))
                    .andExpect(model().attribute("memberships", hasSize(2)));
        }

        @Test
        @WithMockUser(username = ADMIN_EMAIL, roles = "ADMIN")
        @DisplayName("Admin updates premium plan price")
        void savePremiumPlan_updatesPriceInDatabase() throws Exception {
            mockMvc.perform(post("/admin/memberships/" + premiumMembership.getId() + "/save")
                    .with(csrf())
                    .param("name", "Premium")
                    .param("description", premiumMembership.getDescription())
                    .param("price", "34.99")
                    .param("features", premiumMembership.getFeatures()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/memberships"))
                    .andExpect(flash().attributeExists("success"));

            Membership updated = membershipRepository.findById(premiumMembership.getId()).orElseThrow();
            assertThat(updated.getPrice()).isEqualByComparingTo(new BigDecimal("34.99"));
        }

        @Test
        @WithMockUser(username = ADMIN_EMAIL, roles = "ADMIN")
        @DisplayName("Admin updates plan description")
        void savePlan_updatesDescriptionInDatabase() throws Exception {
            mockMvc.perform(post("/admin/memberships/" + premiumMembership.getId() + "/save")
                    .with(csrf())
                    .param("name", "Premium")
                    .param("description", "Updated: full voting rights and annual conference access.")
                    .param("price", premiumMembership.getPrice().toString())
                    .param("features", premiumMembership.getFeatures()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(flash().attributeExists("success"));

            Membership updated = membershipRepository.findById(premiumMembership.getId()).orElseThrow();
            assertThat(updated.getDescription())
                    .isEqualTo("Updated: full voting rights and annual conference access.");
        }

        @Test
        @WithMockUser(username = ADMIN_EMAIL, roles = "ADMIN")
        @DisplayName("Admin updates plan features text")
        void savePlan_updatesFeaturesInDatabase() throws Exception {
            String newFeatures = "- Voting rights\n- Duration: 12 months from signup\n- Priority support";

            mockMvc.perform(post("/admin/memberships/" + premiumMembership.getId() + "/save")
                    .with(csrf())
                    .param("name", "Premium")
                    .param("description", premiumMembership.getDescription())
                    .param("price", premiumMembership.getPrice().toString())
                    .param("features", newFeatures))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(flash().attributeExists("success"));

            Membership updated = membershipRepository.findById(premiumMembership.getId()).orElseThrow();
            assertThat(updated.getFeatures()).contains("Duration: 12 months from signup");
        }

        @Test
        @WithMockUser(username = ADMIN_EMAIL, roles = "ADMIN")
        @DisplayName("Editing free plan does not change zero price")
        void saveFreePlan_keepsPriceAtZero() throws Exception {
            mockMvc.perform(post("/admin/memberships/" + freeMembership.getId() + "/save")
                    .with(csrf())
                    .param("name", "Free Basic")
                    .param("description", "Updated free tier description")
                    .param("price", "99.99")
                    .param("features", "- Basic access only"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(flash().attributeExists("success"));

            Membership updated = membershipRepository.findById(freeMembership.getId()).orElseThrow();
            assertThat(updated.isFree()).isTrue();
            assertThat(updated.getPrice()).isEqualByComparingTo(new BigDecimal("0.00"));
            assertThat(updated.getDescription()).isEqualTo("Updated free tier description");
        }

        @Test
        @WithMockUser(username = ADMIN_EMAIL, roles = "ADMIN")
        @DisplayName("Invalid price format is rejected without updating the plan")
        void saveWithInvalidPrice_redirectsWithError_priceUnchanged() throws Exception {
            BigDecimal before = premiumMembership.getPrice();

            mockMvc.perform(post("/admin/memberships/" + premiumMembership.getId() + "/save")
                    .with(csrf())
                    .param("name", "Premium")
                    .param("description", premiumMembership.getDescription())
                    .param("price", "not-a-number")
                    .param("features", premiumMembership.getFeatures()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/memberships"))
                    .andExpect(flash().attribute("error", containsString("Invalid price format")));

            Membership unchanged = membershipRepository.findById(premiumMembership.getId()).orElseThrow();
            assertThat(unchanged.getPrice()).isEqualByComparingTo(before);
        }

        @Test
        @WithMockUser(username = ADMIN_EMAIL, roles = "ADMIN")
        @DisplayName("Blank required field redirects with error")
        void saveWithBlankName_redirectsWithError() throws Exception {
            mockMvc.perform(post("/admin/memberships/" + premiumMembership.getId() + "/save")
                    .with(csrf())
                    .param("name", "")
                    .param("description", premiumMembership.getDescription())
                    .param("price", premiumMembership.getPrice().toString())
                    .param("features", premiumMembership.getFeatures()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/memberships"))
                    .andExpect(flash().attributeExists("error"));
        }
    }

    @Nested
    @DisplayName("When caller is not an admin")
    class WhenNotAdmin {

        @Test
        @WithMockUser(username = "user@example.com", roles = "USER")
        @DisplayName("Non-admin cannot open membership management")
        void regularUser_getMemberships_forbidden() throws Exception {
            mockMvc.perform(get("/admin/memberships"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "user@example.com", roles = "USER")
        @DisplayName("Non-admin cannot save membership changes")
        void regularUser_saveMembership_forbidden() throws Exception {
            mockMvc.perform(post("/admin/memberships/" + premiumMembership.getId() + "/save")
                    .with(csrf())
                    .param("name", "Premium")
                    .param("description", "Hacked")
                    .param("price", "1.00")
                    .param("features", "X"))
                    .andExpect(status().isForbidden());
        }
    }
}
