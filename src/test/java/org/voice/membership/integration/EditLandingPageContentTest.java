package org.voice.membership.integration;

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
import org.voice.membership.entities.LandingPageContent;
import org.voice.membership.repositories.LandingPageContentRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Use case: Edit Landing Page Content")
class EditLandingPageContentTest {

    private static final String ADMIN_EMAIL = "tarparakrimy1@gmail.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LandingPageContentRepository landingPageContentRepository;

    @Nested
    @DisplayName("When admin is logged in (preconditions satisfied)")
    class WhenAdminIsLoggedIn {

        @Test
        @WithMockUser(username = ADMIN_EMAIL, roles = "ADMIN")
        @DisplayName("Trigger: from dashboard, admin can navigate to the landing page editor")
        void adminDashboard_exposesLinkToLandingPageEditor() throws Exception {
            mockMvc.perform(get("/admin/dashboard"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("/admin/landing-page")));
        }

        @Test
        @WithMockUser(username = ADMIN_EMAIL, roles = "ADMIN")
        @DisplayName("Admin opens the editor and sees all editable text fields (no layout change—same view name)")
        void openEditor_showsLandingPageFormWithContentAttributes() throws Exception {
            mockMvc.perform(get("/admin/landing-page"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin-landing-page"))
                    .andExpect(model().attributeExists("heroTitle"))
                    .andExpect(model().attributeExists("heroTagline"))
                    .andExpect(model().attributeExists("benefitsTitle"))
                    .andExpect(model().attributeExists("reasonsHeading"))
                    .andExpect(model().attributeExists("reasonsContent"));
        }

        @Test
        @WithMockUser(username = ADMIN_EMAIL, roles = "ADMIN")
        @DisplayName("Admin saves updated text; all informational fields are persisted (content keys only)")
        void saveUpdatedText_persistsAllFiveContentKeys() throws Exception {
            mockMvc.perform(post("/admin/landing-page/save")
                    .with(csrf())
                    .param("heroTitle", "UC Hero Title")
                    .param("heroTagline", "UC Hero tagline copy")
                    .param("benefitsTitle", "UC Benefits heading")
                    .param("reasonsHeading", "UC Reasons heading")
                    .param("reasonsContent", "<p>UC informational HTML body</p>"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/landing-page"))
                    .andExpect(flash().attributeExists("success"));

            assertThat(value("hero_title")).isEqualTo("UC Hero Title");
            assertThat(value("hero_tagline")).isEqualTo("UC Hero tagline copy");
            assertThat(value("benefits_title")).isEqualTo("UC Benefits heading");
            assertThat(value("reasons_heading")).isEqualTo("UC Reasons heading");
            assertThat(value("reasons_content")).isEqualTo("<p>UC informational HTML body</p>");
        }

        @Test
        @WithMockUser(username = ADMIN_EMAIL, roles = "ADMIN")
        @DisplayName("Invalid submit (blank field) shows error and does not apply a partial update")
        void blankRequiredField_redirectsWithError_leavesExistingContentUnchanged() throws Exception {
            landingPageServiceSnapshotBeforeInvalidSubmit();

            mockMvc.perform(post("/admin/landing-page/save")
                    .with(csrf())
                    .param("heroTitle", "")
                    .param("heroTagline", "Should not persist alone")
                    .param("benefitsTitle", "Should not persist alone")
                    .param("reasonsHeading", "Should not persist alone")
                    .param("reasonsContent", "<p>Should not persist alone</p>"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/landing-page"))
                    .andExpect(flash().attributeExists("error"));

            assertThat(value("hero_title")).isEqualTo("Snapshot Hero");
            assertThat(value("hero_tagline")).isEqualTo("Snapshot Tagline");
            assertThat(value("benefits_title")).isEqualTo("Snapshot Benefits");
            assertThat(value("reasons_heading")).isEqualTo("Snapshot Reasons Heading");
            assertThat(value("reasons_content")).isEqualTo("<p>Snapshot reasons</p>");
        }

        private void landingPageServiceSnapshotBeforeInvalidSubmit() {
            saveRow("hero_title", "Snapshot Hero");
            saveRow("hero_tagline", "Snapshot Tagline");
            saveRow("benefits_title", "Snapshot Benefits");
            saveRow("reasons_heading", "Snapshot Reasons Heading");
            saveRow("reasons_content", "<p>Snapshot reasons</p>");
        }

        private void saveRow(String key, String val) {
            landingPageContentRepository.findByKey(key).ifPresentOrElse(
                    c -> {
                        c.setValue(val);
                        landingPageContentRepository.save(c);
                    },
                    () -> landingPageContentRepository.save(
                            LandingPageContent.builder()
                                    .key(key)
                                    .value(val)
                                    .active(true)
                                    .build()));
        }
    }

    @Nested
    @DisplayName("When caller is not an admin")
    class WhenNotAdmin {

        @Test
        @WithMockUser(username = "user@example.com", roles = "USER")
        @DisplayName("Non-admin cannot open the landing page editor")
        void regularUser_getEditor_forbidden() throws Exception {
            mockMvc.perform(get("/admin/landing-page"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "user@example.com", roles = "USER")
        @DisplayName("Non-admin cannot save landing page content")
        void regularUser_save_forbidden() throws Exception {
            mockMvc.perform(post("/admin/landing-page/save")
                    .with(csrf())
                    .param("heroTitle", "X")
                    .param("heroTagline", "X")
                    .param("benefitsTitle", "X")
                    .param("reasonsHeading", "X")
                    .param("reasonsContent", "<p>X</p>"))
                    .andExpect(status().isForbidden());
        }
    }

    private String value(String key) {
        return landingPageContentRepository.findByKey(key)
                .map(LandingPageContent::getValue)
                .orElse(null);
    }
}
