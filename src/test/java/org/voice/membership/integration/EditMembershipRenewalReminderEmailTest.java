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
@DisplayName("Use case: Edit Membership Renewal Reminder Email")
class EditMembershipRenewalReminderEmailTest {

    private static final String ADMIN_EMAIL = "tarparakrimy1@gmail.com";
    private static final String KEY_SUBJECT = "renewal_email_subject";
    private static final String KEY_BODY = "renewal_email_body";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LandingPageContentRepository landingPageContentRepository;

    @BeforeEach
    void ensureRenewalTemplateExists() {
        putContent(KEY_SUBJECT, "Subject: Your membership expires in {daysUntilExpiry} day(s)");
        putContent(KEY_BODY,
                "Hello {memberName},\n\nYour {membershipName} expires on {expiryDate}.\nRenew: {renewalUrl}\n");
    }

    private void putContent(String key, String value) {
        landingPageContentRepository.findByKey(key).ifPresentOrElse(
                c -> {
                    c.setValue(value);
                    landingPageContentRepository.save(c);
                },
                () -> landingPageContentRepository.save(
                        LandingPageContent.builder()
                                .key(key)
                                .value(value)
                                .active(true)
                                .build()));
    }

    @Nested
    @DisplayName("When admin is logged in and template exists")
    class WhenAdminAndTemplateExists {

        @Test
        @WithMockUser(username = ADMIN_EMAIL, roles = "ADMIN")
        @DisplayName("Trigger: dashboard links to renewal reminder email editor")
        void adminDashboard_exposesLinkToRenewalEmailEditor() throws Exception {
            mockMvc.perform(get("/admin/dashboard"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("/admin/renewal-email")));
        }

        @Test
        @WithMockUser(username = ADMIN_EMAIL, roles = "ADMIN")
        @DisplayName("Admin opens editor and sees subject and body fields")
        void openEditor_showsRenewalEmailForm() throws Exception {
            mockMvc.perform(get("/admin/renewal-email"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin-renewal-email"))
                    .andExpect(model().attributeExists("renewalSubject"))
                    .andExpect(model().attributeExists("renewalBody"))
                    .andExpect(model().attribute("renewalSubject",
                            containsString("{daysUntilExpiry}")))
                    .andExpect(model().attribute("renewalBody", containsString("{memberName}")));
        }

        @Test
        @WithMockUser(username = ADMIN_EMAIL, roles = "ADMIN")
        @DisplayName("Admin saves new subject and body; content keys are updated")
        void saveValidForm_persistsSubjectAndBody() throws Exception {
            mockMvc.perform(post("/admin/renewal-email/save")
                    .with(csrf())
                    .param("renewalSubject", "Reminder: renew before {expiryDate}")
                    .param("renewalBody",
                            "Hi {memberName},\nPlease renew your {membershipName} by {expiryDate}.\nLink: {renewalUrl}\n"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/renewal-email"))
                    .andExpect(flash().attributeExists("success"));

            assertThat(contentValue(KEY_SUBJECT)).isEqualTo("Reminder: renew before {expiryDate}");
            assertThat(contentValue(KEY_BODY)).isEqualTo(
                    "Hi {memberName},\nPlease renew your {membershipName} by {expiryDate}.\nLink: {renewalUrl}");
        }

        @Test
        @WithMockUser(username = ADMIN_EMAIL, roles = "ADMIN")
        @DisplayName("Editor shows latest saved subject and body after redirect")
        void afterSave_editorDisplaysUpdatedTemplate() throws Exception {
            mockMvc.perform(post("/admin/renewal-email/save")
                    .with(csrf())
                    .param("renewalSubject", "Updated subject line")
                    .param("renewalBody", "Updated body with {renewalUrl}"))
                    .andExpect(status().is3xxRedirection());

            mockMvc.perform(get("/admin/renewal-email"))
                    .andExpect(status().isOk())
                    .andExpect(model().attribute("renewalSubject", "Updated subject line"))
                    .andExpect(model().attribute("renewalBody", "Updated body with {renewalUrl}"));
        }

        @Test
        @WithMockUser(username = ADMIN_EMAIL, roles = "ADMIN")
        @DisplayName("Blank subject redirects with error and does not update stored template")
        void blankSubject_redirectsWithError_noUpdate() throws Exception {
            String subjectBefore = contentValue(KEY_SUBJECT);
            String bodyBefore = contentValue(KEY_BODY);

            mockMvc.perform(post("/admin/renewal-email/save")
                    .with(csrf())
                    .param("renewalSubject", "   ")
                    .param("renewalBody", "Only body changed attempt"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/renewal-email"))
                    .andExpect(flash().attributeExists("error"));

            assertThat(contentValue(KEY_SUBJECT)).isEqualTo(subjectBefore);
            assertThat(contentValue(KEY_BODY)).isEqualTo(bodyBefore);
        }

        @Test
        @WithMockUser(username = ADMIN_EMAIL, roles = "ADMIN")
        @DisplayName("Blank body redirects with error and does not update stored template")
        void blankBody_redirectsWithError_noUpdate() throws Exception {
            String subjectBefore = contentValue(KEY_SUBJECT);
            String bodyBefore = contentValue(KEY_BODY);

            mockMvc.perform(post("/admin/renewal-email/save")
                    .with(csrf())
                    .param("renewalSubject", "Valid subject")
                    .param("renewalBody", ""))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/renewal-email"))
                    .andExpect(flash().attributeExists("error"));

            assertThat(contentValue(KEY_SUBJECT)).isEqualTo(subjectBefore);
            assertThat(contentValue(KEY_BODY)).isEqualTo(bodyBefore);
        }
    }

    @Nested
    @DisplayName("When caller is not an admin")
    class WhenNotAdmin {

        @Test
        @WithMockUser(username = "user@example.com", roles = "USER")
        @DisplayName("Non-admin cannot open renewal email editor")
        void regularUser_getEditor_forbidden() throws Exception {
            mockMvc.perform(get("/admin/renewal-email"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "user@example.com", roles = "USER")
        @DisplayName("Non-admin cannot save renewal email template")
        void regularUser_save_forbidden() throws Exception {
            mockMvc.perform(post("/admin/renewal-email/save")
                    .with(csrf())
                    .param("renewalSubject", "X")
                    .param("renewalBody", "Y"))
                    .andExpect(status().isForbidden());
        }
    }

    private String contentValue(String key) {
        return landingPageContentRepository.findByKey(key)
                .map(LandingPageContent::getValue)
                .orElse(null);
    }
}
