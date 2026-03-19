package org.voice.membership.services;

import lombok.extern.slf4j.Slf4j;
import org.voice.membership.entities.LandingPageContent;
import org.voice.membership.entities.Membership;
import org.voice.membership.repositories.LandingPageContentRepository;
import org.voice.membership.repositories.MembershipRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    private LandingPageContentRepository landingPageContentRepository;

    public List<Membership> getActiveMemberships() {
        return membershipRepository.findByActiveTrueOrderByDisplayOrderAsc();
    }

    public String getContentByKey(String key) {
        return landingPageContentRepository.findByKey(key)
                .map(LandingPageContent::getValue)
                .orElse("");
    }

    public String getTagline() {
        return getContentByKey("tagline");
    }

    public String getHeroTitle() {
        return getContentByKey("hero_title");
    }

    public String getHeroTagline() {
        return getContentByKey("hero_tagline");
    }

    public String getBenefitsTitle() {
        return getContentByKey("benefits_title");
    }

    public String getReasonsHeading() {
        return getContentByKey("reasons_heading");
    }

    public String getReasonsContent() {
        return getContentByKey("reasons_content");
    }

    public Map<String, String> getAllContent() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("hero_title", getHeroTitle());
        map.put("hero_tagline", getHeroTagline());
        map.put("benefits_title", getBenefitsTitle());
        map.put("reasons_heading", getReasonsHeading());
        map.put("reasons_content", getReasonsContent());
        return map;
    }

    public void updateContent(String key, String value) {
        LandingPageContent content = landingPageContentRepository.findByKey(key)
                .orElse(LandingPageContent.builder().key(key).active(true).build());
        content.setValue(value);
        landingPageContentRepository.save(content);
    }

    public void initializeDefaultContent() {
        if (landingPageContentRepository.findByKey("tagline").isEmpty()) {
            landingPageContentRepository.save(LandingPageContent.builder()
                    .key("tagline")
                    .value("Empowering families of children who are Deaf and Hard of Hearing")
                    .active(true).build());
        }
        if (landingPageContentRepository.findByKey("hero_title").isEmpty()) {
            landingPageContentRepository.save(LandingPageContent.builder()
                    .key("hero_title")
                    .value("Empowering Families of Children Who Are Deaf and Hard of Hearing")
                    .active(true).build());
        }
        if (landingPageContentRepository.findByKey("hero_tagline").isEmpty()) {
            landingPageContentRepository.save(LandingPageContent.builder()
                    .key("hero_tagline")
                    .value("Join our supportive community and access resources, programs, and connections that make a real difference in your family's journey.")
                    .active(true).build());
        }
        if (landingPageContentRepository.findByKey("benefits_title").isEmpty()) {
            landingPageContentRepository.save(LandingPageContent.builder()
                    .key("benefits_title")
                    .value("Why Join VOICE?")
                    .active(true).build());
        }
        if (landingPageContentRepository.findByKey("reasons_heading").isEmpty()) {
            landingPageContentRepository.save(LandingPageContent.builder()
                    .key("reasons_heading")
                    .value("10 Great Reasons to Be a Member")
                    .active(true).build());
        }
        if (landingPageContentRepository.findByKey("reasons_content").isEmpty()) {
            String defaultReasons = "<p><strong>1. Connection:</strong> You are connected to a larger group banded together to produce positive results for children experiencing hearing loss in our province.</p>\n"
                    +
                    "<p><strong>2. Annual Educational Conference:</strong> The annual Conference provides quality education and networking for those who have experienced hearing loss in their lives, or the lives of others, and you receive a SIGNIFICANT discount by being a member. You can contribute to the content of the conference, participate as a volunteer, or attend to enhance your professional development.</p>\n"
                    +
                    "<p><strong>3. Public Policy / Advocacy:</strong> You can contribute to the strong advocacy program centered on assuring that the voices of those who are experiencing hearing loss are being heard.</p>\n"
                    +
                    "<p><strong>4. Free Financial &amp; Informational Services:</strong> VOICE provides their members with the opportunity to access Financial Aid such as Funds dedicated to helping families provide their children with hearing technology, as well as various scholarships to assist in educational endeavours. VOICE keeps members in touch with each other and current issues through our web site and email updates of emerging issues relevant to hearing loss. You remain informed about hearing loss in our region in a way that saves you time.</p>\n"
                    +
                    "<p><strong>5. Networking:</strong> You have many opportunities to network with like-minded individuals and professionals in our region, building your contacts, sharing ideas, best practices and solutions to enhance your learning, find opportunities to connect, and more. Networking events range from Annual Conferences to workshops, and activities for your children.</p>\n"
                    +
                    "<p><strong>6. Affiliation:</strong> You will have affiliation with a local chapter and dozens of professionals in your region.</p>\n"
                    +
                    "<p><strong>7. Discounts on Educational Offerings:</strong> You receive reduced registration fees at all VOICE events and workshops.</p>\n"
                    +
                    "<p><strong>8. Leadership Opportunities:</strong> You are provided opportunities to learn and practice leadership by becoming an integral part of VOICE whether that be getting involved in your chapter, starting a new chapter, volunteering at events or hosting your own events!</p>\n"
                    +
                    "<p><strong>9. Recognition:</strong> You can be recognized or can recognize your colleagues for their achievements in the hearing loss community, whether that be at VOICE or another affiliated organization.</p>\n"
                    +
                    "<p><strong>10. Empowerment:</strong> You are part of a larger community. You are empowered to get involved in issues that affect your personal and professional communities and your quality of life.</p>";
            landingPageContentRepository.save(LandingPageContent.builder()
                    .key("reasons_content")
                    .value(defaultReasons)
                    .active(true).build());
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

}
