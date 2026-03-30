package org.voice.membership.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.voice.membership.entities.LandingPageContent;
import org.voice.membership.entities.Membership;
import org.voice.membership.repositories.LandingPageContentRepository;
import org.voice.membership.repositories.MembershipRepository;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for LandingPageService
 * Tests landing page content retrieval, update, and initialization logic
 */
@ExtendWith(MockitoExtension.class)
class LandingPageServiceTest {

    @Mock
    private MembershipRepository membershipRepository;

    @Mock
    private LandingPageContentRepository landingPageContentRepository;

    @InjectMocks
    private LandingPageService landingPageService;

    private LandingPageContent taglineContent;
    private LandingPageContent heroTitleContent;
    private LandingPageContent heroTaglineContent;
    private LandingPageContent benefitsTitleContent;
    private LandingPageContent reasonsHeadingContent;
    private LandingPageContent renewalEmailSubjectContent;
    private LandingPageContent renewalEmailBodyContent;
    private LandingPageContent reasonsContentContent;
    private Membership freeMembership;
    private Membership premiumMembership;

    @BeforeEach
    void setUp() {
        taglineContent = LandingPageContent.builder()
                .id(0).key("tagline")
                .value("Empowering families of children who are Deaf and Hard of Hearing").active(true).build();

        heroTitleContent = LandingPageContent.builder()
                .id(1).key("hero_title")
                .value("Welcome to VOICE").active(true).build();

        heroTaglineContent = LandingPageContent.builder()
                .id(2).key("hero_tagline")
                .value("Empowering families of Deaf and Hard of Hearing children").active(true).build();

        benefitsTitleContent = LandingPageContent.builder()
                .id(3).key("benefits_title")
                .value("Why Join VOICE?").active(true).build();

        reasonsHeadingContent = LandingPageContent.builder()
                .id(4).key("reasons_heading")
                .value("10 Great Reasons to Join").active(true).build();

        renewalEmailSubjectContent = LandingPageContent.builder()
                .id(6).key("renewal_email_subject")
                .value("Your VOICE Membership Expires").active(true).build();

        renewalEmailBodyContent = LandingPageContent.builder()
                .id(7).key("renewal_email_body")
                .value("Renewal email body").active(true).build();

        reasonsContentContent = LandingPageContent.builder()
                .id(5).key("reasons_content")
                .value("<ol><li>Community Support</li></ol>").active(true).build();

        freeMembership = Membership.builder()
                .id(1).name("Free").description("Basic membership")
                .price(BigDecimal.ZERO).isFree(true).displayOrder(1).active(true).build();

        premiumMembership = Membership.builder()
                .id(2).name("Premium").description("Full membership")
                .price(new BigDecimal("20.00")).isFree(false).displayOrder(2).active(true).build();
    }

    // ========================== Positive Test Cases ==========================

    @Test
    void getHeroTitle_WhenContentExists_ShouldReturnValue() {
        when(landingPageContentRepository.findByKey("hero_title"))
                .thenReturn(Optional.of(heroTitleContent));

        String result = landingPageService.getHeroTitle();

        assertThat(result).isEqualTo("Welcome to VOICE");
        verify(landingPageContentRepository).findByKey("hero_title");
    }

    @Test
    void getHeroTagline_WhenContentExists_ShouldReturnValue() {
        when(landingPageContentRepository.findByKey("hero_tagline"))
                .thenReturn(Optional.of(heroTaglineContent));

        String result = landingPageService.getHeroTagline();

        assertThat(result).isEqualTo("Empowering families of Deaf and Hard of Hearing children");
        verify(landingPageContentRepository).findByKey("hero_tagline");
    }

    @Test
    void getBenefitsTitle_WhenContentExists_ShouldReturnValue() {
        when(landingPageContentRepository.findByKey("benefits_title"))
                .thenReturn(Optional.of(benefitsTitleContent));

        String result = landingPageService.getBenefitsTitle();

        assertThat(result).isEqualTo("Why Join VOICE?");
    }

    @Test
    void getReasonsHeading_WhenContentExists_ShouldReturnValue() {
        when(landingPageContentRepository.findByKey("reasons_heading"))
                .thenReturn(Optional.of(reasonsHeadingContent));

        String result = landingPageService.getReasonsHeading();

        assertThat(result).isEqualTo("10 Great Reasons to Join");
    }

    @Test
    void getReasonsContent_WhenContentExists_ShouldReturnValue() {
        when(landingPageContentRepository.findByKey("reasons_content"))
                .thenReturn(Optional.of(reasonsContentContent));

        String result = landingPageService.getReasonsContent();

        assertThat(result).isEqualTo("<ol><li>Community Support</li></ol>");
    }

    @Test
    void updateContent_WhenKeyExists_ShouldUpdateValue() {
        when(landingPageContentRepository.findByKey("hero_title"))
                .thenReturn(Optional.of(heroTitleContent));
        when(landingPageContentRepository.save(any(LandingPageContent.class)))
                .thenAnswer(i -> i.getArguments()[0]);

        landingPageService.updateContent("hero_title", "New Hero Title");

        verify(landingPageContentRepository)
                .save(argThat(c -> "hero_title".equals(c.getKey()) && "New Hero Title".equals(c.getValue())));
    }

    @Test
    void updateContent_WhenKeyNotExists_ShouldCreateNewEntry() {
        when(landingPageContentRepository.findByKey("hero_title"))
                .thenReturn(Optional.empty());
        when(landingPageContentRepository.save(any(LandingPageContent.class)))
                .thenAnswer(i -> i.getArguments()[0]);

        landingPageService.updateContent("hero_title", "Brand New Title");

        verify(landingPageContentRepository)
                .save(argThat(c -> "hero_title".equals(c.getKey()) && "Brand New Title".equals(c.getValue())));
    }

    @Test
    void getAllContent_ShouldReturnMapWithAllFiveKeys() {
        when(landingPageContentRepository.findByKey("hero_title")).thenReturn(Optional.of(heroTitleContent));
        when(landingPageContentRepository.findByKey("hero_tagline")).thenReturn(Optional.of(heroTaglineContent));
        when(landingPageContentRepository.findByKey("benefits_title")).thenReturn(Optional.of(benefitsTitleContent));
        when(landingPageContentRepository.findByKey("reasons_heading")).thenReturn(Optional.of(reasonsHeadingContent));
        when(landingPageContentRepository.findByKey("reasons_content")).thenReturn(Optional.of(reasonsContentContent));

        Map<String, String> result = landingPageService.getAllContent();

        assertThat(result).containsKeys("hero_title", "hero_tagline", "benefits_title", "reasons_heading",
                "reasons_content");
        assertThat(result.get("hero_title")).isEqualTo("Welcome to VOICE");
        assertThat(result.get("hero_tagline")).isEqualTo("Empowering families of Deaf and Hard of Hearing children");
    }

    @Test
    void getActiveMemberships_ShouldReturnOrderedList() {
        when(membershipRepository.findByActiveTrueOrderByDisplayOrderAsc())
                .thenReturn(Arrays.asList(freeMembership, premiumMembership));

        List<Membership> result = landingPageService.getActiveMemberships();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Free");
        assertThat(result.get(1).getName()).isEqualTo("Premium");
        verify(membershipRepository).findByActiveTrueOrderByDisplayOrderAsc();
    }

    // ========================== Negative Test Cases ==========================

    @Test
    void getHeroTitle_WhenContentNotExists_ShouldReturnEmptyString() {
        when(landingPageContentRepository.findByKey("hero_title"))
                .thenReturn(Optional.empty());

        String result = landingPageService.getHeroTitle();

        assertThat(result).isEqualTo("");
    }

    @Test
    void getHeroTagline_WhenContentNotExists_ShouldReturnEmptyString() {
        when(landingPageContentRepository.findByKey("hero_tagline"))
                .thenReturn(Optional.empty());

        String result = landingPageService.getHeroTagline();

        assertThat(result).isEqualTo("");
    }

    @Test
    void getContentByKey_WithNonExistentKey_ShouldReturnEmptyString() {
        when(landingPageContentRepository.findByKey("nonexistent_key"))
                .thenReturn(Optional.empty());

        String result = landingPageService.getContentByKey("nonexistent_key");

        assertThat(result).isEqualTo("");
    }

    @Test
    void getActiveMemberships_WhenNoneActive_ShouldReturnEmptyList() {
        when(membershipRepository.findByActiveTrueOrderByDisplayOrderAsc())
                .thenReturn(List.of());

        List<Membership> result = landingPageService.getActiveMemberships();

        assertThat(result).isEmpty();
    }

    @Test
    void updateContent_WithEmptyValue_ShouldSaveEmptyString() {
        when(landingPageContentRepository.findByKey("hero_title"))
                .thenReturn(Optional.of(heroTitleContent));
        when(landingPageContentRepository.save(any(LandingPageContent.class)))
                .thenAnswer(i -> i.getArguments()[0]);

        landingPageService.updateContent("hero_title", "");

        verify(landingPageContentRepository)
                .save(argThat(c -> "hero_title".equals(c.getKey()) && "".equals(c.getValue())));
    }

    @Test
    void initializeDefaultContent_WhenContentAlreadyExists_ShouldNotOverwrite() {
        when(landingPageContentRepository.findByKey("tagline"))
                .thenReturn(Optional.of(taglineContent));
        when(landingPageContentRepository.findByKey("hero_title"))
                .thenReturn(Optional.of(heroTitleContent));
        when(landingPageContentRepository.findByKey("hero_tagline"))
                .thenReturn(Optional.of(heroTaglineContent));
        when(landingPageContentRepository.findByKey("benefits_title"))
                .thenReturn(Optional.of(benefitsTitleContent));
        when(landingPageContentRepository.findByKey("reasons_heading"))
                .thenReturn(Optional.of(reasonsHeadingContent));
        when(landingPageContentRepository.findByKey("renewal_email_subject"))
                .thenReturn(Optional.of(renewalEmailSubjectContent));
        when(landingPageContentRepository.findByKey("renewal_email_body"))
                .thenReturn(Optional.of(renewalEmailBodyContent));
        when(landingPageContentRepository.findByKey("reasons_content"))
                .thenReturn(Optional.of(reasonsContentContent));

        landingPageService.initializeDefaultContent();

        verify(landingPageContentRepository, never()).save(any(LandingPageContent.class));
    }

    @Test
    void initializeDefaultContent_WhenContentNotExists_ShouldCreateDefaultContent() {
        when(landingPageContentRepository.findByKey("tagline"))
                .thenReturn(Optional.empty());
        when(landingPageContentRepository.save(any(LandingPageContent.class)))
                .thenAnswer(i -> i.getArguments()[0]);

        landingPageService.initializeDefaultContent();

        verify(landingPageContentRepository, atLeastOnce()).save(any(LandingPageContent.class));
    }
}
