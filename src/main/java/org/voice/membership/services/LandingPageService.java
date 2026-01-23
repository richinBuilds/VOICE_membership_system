package org.voice.membership.services;

import org.voice.membership.entities.LandingPageContent;
import org.voice.membership.entities.Membership;
import org.voice.membership.entities.MembershipBenefit;
import org.voice.membership.repositories.LandingPageContentRepository;
import org.voice.membership.repositories.MembershipBenefitRepository;
import org.voice.membership.repositories.MembershipRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LandingPageService {

    @Autowired
    private MembershipRepository membershipRepository;

    @Autowired
    private MembershipBenefitRepository membershipBenefitRepository;

    @Autowired
    private LandingPageContentRepository landingPageContentRepository;

    /**
     * Get all active membership options ordered by display order
     */
    public List<Membership> getActiveMemberships() {
        return membershipRepository.findByActiveTrueOrderByDisplayOrderAsc();
    }

    /**
     * Get all active membership benefits ordered by display order
     */
    public List<MembershipBenefit> getActiveBenefits() {
        return membershipBenefitRepository.findByActiveTrueOrderByDisplayOrderAsc();
    }

    /**
     * Get landing page content by key (mission, subtitle, etc.)
     */
    public String getContentByKey(String key) {
        return landingPageContentRepository.findByKey(key)
                .map(LandingPageContent::getValue)
                .orElse("");
    }

    /**
     * Get landing page subtitle/tagline
     */
    public String getTagline() {
        return getContentByKey("tagline");
    }

    /**
     * Initialize default landing page content if it doesn't exist
     */
    public void initializeDefaultContent() {
        if (landingPageContentRepository.findByKey("tagline").isEmpty()) {
            LandingPageContent taglineContent = LandingPageContent.builder()
                    .key("tagline")
                    .value("Empowering families of children who are Deaf and Hard of Hearing")
                    .active(true)
                    .build();
            landingPageContentRepository.save(taglineContent);
        }
    }

    /**
     * Initialize default membership options if they don't exist
     */
    public void initializeDefaultMemberships() {
        long membershipCount = membershipRepository.count();
        if (membershipCount == 0) {
            // Free membership
            Membership freeMembership = Membership.builder()
                    .name("Free")
                    .description("Get started with VOICE community")
                    .price(null)
                    .features("Basic access\nCommunity forum access\nWeekly newsletters\nBasic profile")
                    .isFree(true)
                    .displayOrder(1)
                    .active(true)
                    .build();
            membershipRepository.save(freeMembership);

        }
    }

    /**
     * Initialize default membership benefits if they don't exist
     */
    public void initializeDefaultBenefits() {
        long benefitCount = membershipBenefitRepository.count();
        if (benefitCount == 0) {
            MembershipBenefit benefit1 = MembershipBenefit.builder()
                    .title("Community Network")
                    .description("Connect with like-minded professionals and innovators")
                    .icon("fa-users")
                    .displayOrder(1)
                    .active(true)
                    .build();
            membershipBenefitRepository.save(benefit1);

            MembershipBenefit benefit2 = MembershipBenefit.builder()
                    .title("Exclusive Content")
                    .description("Access to premium articles, webinars, and resources")
                    .icon("fa-book")
                    .displayOrder(2)
                    .active(true)
                    .build();
            membershipBenefitRepository.save(benefit2);

            MembershipBenefit benefit3 = MembershipBenefit.builder()
                    .title("Career Opportunities")
                    .description("Find jobs, internships, and collaboration opportunities")
                    .icon("fa-briefcase")
                    .displayOrder(3)
                    .active(true)
                    .build();
            membershipBenefitRepository.save(benefit3);

            MembershipBenefit benefit4 = MembershipBenefit.builder()
                    .title("Skill Development")
                    .description("Participate in workshops and training programs")
                    .icon("fa-graduation-cap")
                    .displayOrder(4)
                    .active(true)
                    .build();
            membershipBenefitRepository.save(benefit4);

            MembershipBenefit benefit5 = MembershipBenefit.builder()
                    .title("24/7 Support")
                    .description("Get help when you need it from our support team")
                    .icon("fa-headset")
                    .displayOrder(5)
                    .active(true)
                    .build();
            membershipBenefitRepository.save(benefit5);
        }
    }
}
