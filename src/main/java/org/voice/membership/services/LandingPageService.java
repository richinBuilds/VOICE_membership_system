package org.voice.membership.services;

import lombok.extern.slf4j.Slf4j;
import org.voice.membership.entities.LandingPageContent;
import org.voice.membership.entities.Membership;
import org.voice.membership.entities.MembershipBenefit;
import org.voice.membership.repositories.LandingPageContentRepository;
import org.voice.membership.repositories.MembershipBenefitRepository;
import org.voice.membership.repositories.MembershipRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 
 * Business logic service for landing page and membership initialization.
 * Retrieves and manages: memberships, benefits, and landing page content.
 * Initializes default data (Free + Premium memberships, benefits, tagline) on
 * app startup.
 * Provides methods to populate database with seed data if not already present
 * (idempotent).
 */
@Slf4j
@Service
public class LandingPageService {

    @Autowired
    private MembershipRepository membershipRepository;

    @Autowired
    private MembershipBenefitRepository membershipBenefitRepository;

    @Autowired
    private LandingPageContentRepository landingPageContentRepository;

    public List<Membership> getActiveMemberships() {
        return membershipRepository.findByActiveTrueOrderByDisplayOrderAsc();
    }

    public List<MembershipBenefit> getActiveBenefits() {
        return membershipBenefitRepository.findByActiveTrueOrderByDisplayOrderAsc();
    }

    public String getContentByKey(String key) {
        return landingPageContentRepository.findByKey(key)
                .map(LandingPageContent::getValue)
                .orElse("");
    }

    public String getTagline() {
        return getContentByKey("tagline");
    }

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

    public void initializeDefaultMemberships() {
        log.info("Initializing default memberships...");
        
        // Check for Free membership
        List<Membership> freeMemberships = membershipRepository.findByNameAndIsFreeTrue("Free");
        if (freeMemberships.isEmpty()) {
            log.info("Creating Free membership...");
            Membership freeMembership = Membership.builder()
                    .name("Free")
                    .description("Get started with VOICE community")
                    .price(null)
                    .features(
                            "Basic access" + System.lineSeparator() + "Community forum access" + System.lineSeparator()
                                    + "Weekly newsletters" + System.lineSeparator() + "No voting rights")
                    .isFree(true)
                    .displayOrder(1)
                    .active(true)
                    .build();
            membershipRepository.save(freeMembership);
            log.info("Free membership created successfully");
        } else {
            log.info("Free membership already exists");
            // Ensure existing free membership is active
            Membership existing = freeMemberships.get(0);
            if (!existing.isActive()) {
                log.info("Activating existing Free membership");
                existing.setActive(true);
                membershipRepository.save(existing);
            }
        }

        // Check for Premium membership
        List<Membership> premiumMemberships = membershipRepository.findByNameAndIsFreeFalse("Premium");
        if (premiumMemberships.isEmpty()) {
            log.info("Creating Premium membership...");
            Membership paidMembership = Membership.builder()
                    .name("Premium")
                    .description("Support VOICE and unlock premium benefits")
                    .price(new java.math.BigDecimal("20.00"))
                    .features("Membership with full voting right" + System.lineSeparator()
                            + "Includes two adults and any minor dependents in the same household"
                            + System.lineSeparator() + "Exclusive webinars" + System.lineSeparator()
                            + "Updated on events and kept informed")

                    .isFree(false)
                    .displayOrder(2)
                    .active(true)
                    .build();
            membershipRepository.save(paidMembership);
            log.info("Premium membership created successfully");
        } else {
            log.info("Premium membership already exists");
            // Ensure existing premium membership is active
            Membership existing = premiumMemberships.get(0);
            if (!existing.isActive()) {
                log.info("Activating existing Premium membership");
                existing.setActive(true);
                membershipRepository.save(existing);
            }
        }
        
        // Log all active memberships
        List<Membership> activeMemberships = membershipRepository.findByActiveTrueOrderByDisplayOrderAsc();
        log.info("Total active memberships: {}", activeMemberships.size());
        for (Membership m : activeMemberships) {
            log.info("  - {} (isFree={}, active={}, price={})", m.getName(), m.isFree(), m.isActive(), m.getPrice());
        }
    }

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

