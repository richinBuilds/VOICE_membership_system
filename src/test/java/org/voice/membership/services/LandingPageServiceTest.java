package org.voice.membership.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.voice.membership.entities.LandingPageContent;
import org.voice.membership.entities.Membership;
import org.voice.membership.entities.MembershipBenefit;
import org.voice.membership.repositories.LandingPageContentRepository;
import org.voice.membership.repositories.MembershipBenefitRepository;
import org.voice.membership.repositories.MembershipRepository;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for LandingPageService
 * Tests membership and landing page content initialization
 */
@ExtendWith(MockitoExtension.class)
class LandingPageServiceTest {

    @Mock
    private MembershipRepository membershipRepository;

    @Mock
    private MembershipBenefitRepository membershipBenefitRepository;

    @Mock
    private LandingPageContentRepository landingPageContentRepository;

    @InjectMocks
    private LandingPageService landingPageService;

    private Membership freeMembership;
    private Membership premiumMembership;
    private List<MembershipBenefit> benefits;
    private LandingPageContent landingPageContent;

    @BeforeEach
    void setUp() {
        freeMembership = Membership.builder()
                .id(1)
                .name("Free")
                .description("Basic membership")
                .price(BigDecimal.ZERO)
                .isFree(true)
                .displayOrder(1)
                .active(true)
                .build();

        premiumMembership = Membership.builder()
                .id(2)
                .name("Premium")
                .description("Premium membership with full benefits")
                .price(new BigDecimal("20.00"))
                .isFree(false)
                .displayOrder(2)
                .active(true)
                .build();

        MembershipBenefit benefit1 = MembershipBenefit.builder()
                .id(1)
                .title("Community Network")
                .description("Connect with other families")
                .icon("fa-users")
                .displayOrder(1)
                .active(true)
                .build();

        MembershipBenefit benefit2 = MembershipBenefit.builder()
                .id(2)
                .title("Resources")
                .description("Access educational materials")
                .icon("fa-book")
                .displayOrder(2)
                .active(true)
                .build();

        benefits = Arrays.asList(benefit1, benefit2);

        landingPageContent = LandingPageContent.builder()
                .id(1)
                .key("tagline")
                .value("Empowering Families with Deaf Children")
                .active(true)
                .build();
    }

    @Test
    void getActiveMemberships_ShouldReturnListOfMemberships() {
        when(membershipRepository.findByActiveTrueOrderByDisplayOrderAsc())
                .thenReturn(Arrays.asList(freeMembership, premiumMembership));

        List<Membership> result = landingPageService.getActiveMemberships();

        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).contains(freeMembership, premiumMembership);
        verify(membershipRepository).findByActiveTrueOrderByDisplayOrderAsc();
    }

    @Test
    void getActiveBenefits_ShouldReturnListOfBenefits() {
        when(membershipBenefitRepository.findByActiveTrueOrderByDisplayOrderAsc()).thenReturn(benefits);

        List<MembershipBenefit> result = landingPageService.getActiveBenefits();

        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTitle()).isEqualTo("Community Network");
        verify(membershipBenefitRepository).findByActiveTrueOrderByDisplayOrderAsc();
    }

    @Test
    void getTagline_ShouldReturnTaglineString() {
        when(landingPageContentRepository.findByKey("tagline")).thenReturn(java.util.Optional.of(landingPageContent));

        String result = landingPageService.getTagline();

        assertThat(result).isNotNull();
        assertThat(result).isEqualTo("Empowering Families with Deaf Children");
        verify(landingPageContentRepository).findByKey("tagline");
    }

    @Test
    void initializeDefaultMemberships_WhenNoMembershipsExist_ShouldCreateMemberships() {
        when(membershipRepository.count()).thenReturn(0L);
        when(membershipRepository.save(any(Membership.class))).thenAnswer(i -> i.getArguments()[0]);

        landingPageService.initializeDefaultMemberships();

        verify(membershipRepository, times(2)).save(any(Membership.class));
    }

    @Test
    void initializeDefaultMemberships_WhenMembershipsExist_ShouldNotCreateMemberships() {
        when(membershipRepository.count()).thenReturn(2L);

        landingPageService.initializeDefaultMemberships();

        verify(membershipRepository, never()).save(any(Membership.class));
    }

    @Test
    void initializeDefaultBenefits_WhenNoBenefitsExist_ShouldCreateBenefits() {
        when(membershipBenefitRepository.count()).thenReturn(0L);
        when(membershipBenefitRepository.save(any(MembershipBenefit.class))).thenAnswer(i -> i.getArguments()[0]);

        landingPageService.initializeDefaultBenefits();

        verify(membershipBenefitRepository, times(5)).save(any(MembershipBenefit.class));
    }

    @Test
    void initializeDefaultBenefits_WhenBenefitsExist_ShouldNotCreateBenefits() {
        when(membershipBenefitRepository.count()).thenReturn(6L);

        landingPageService.initializeDefaultBenefits();

        verify(membershipBenefitRepository, never()).save(any(MembershipBenefit.class));
    }

    @Test
    void initializeDefaultContent_WhenNoContentExists_ShouldCreateContent() {
        when(landingPageContentRepository.findByKey("tagline")).thenReturn(java.util.Optional.empty());
        when(landingPageContentRepository.save(any(LandingPageContent.class))).thenAnswer(i -> i.getArguments()[0]);

        landingPageService.initializeDefaultContent();

        verify(landingPageContentRepository).save(any(LandingPageContent.class));
    }

    @Test
    void initializeDefaultContent_WhenContentExists_ShouldNotCreateContent() {
        when(landingPageContentRepository.findByKey("tagline")).thenReturn(java.util.Optional.of(landingPageContent));

        landingPageService.initializeDefaultContent();

        verify(landingPageContentRepository, never()).save(any(LandingPageContent.class));
    }
}

