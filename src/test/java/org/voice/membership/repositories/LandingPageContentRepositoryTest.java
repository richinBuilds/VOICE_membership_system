package org.voice.membership.repositories;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.voice.membership.entities.LandingPageContent;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for LandingPageContentRepository
 * Tests database operations for landing page content
 */
@DataJpaTest
@ActiveProfiles("test")
class LandingPageContentRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private LandingPageContentRepository landingPageContentRepository;

    private LandingPageContent heroTitle;
    private LandingPageContent heroSubtitle;
    private LandingPageContent aboutSection;

    @BeforeEach
    void setUp() {
        heroTitle = new LandingPageContent();
        heroTitle.setKey("hero.title");
        heroTitle.setValue("Welcome to VOICE");
        heroTitle.setActive(true);

        heroSubtitle = new LandingPageContent();
        heroSubtitle.setKey("hero.subtitle");
        heroSubtitle.setValue("Supporting families of children who are deaf or hard of hearing");
        heroSubtitle.setActive(true);

        aboutSection = new LandingPageContent();
        aboutSection.setKey("about.section");
        aboutSection.setValue("VOICE is a non-profit organization dedicated to supporting families...");
        aboutSection.setActive(true);
    }

    @Test
    void findByKey_WithExistingKey_ShouldReturnContent() {
        entityManager.persist(heroTitle);
        entityManager.flush();

        Optional<LandingPageContent> found = landingPageContentRepository.findByKey("hero.title");

        assertThat(found).isPresent();
        assertThat(found.get().getKey()).isEqualTo("hero.title");
        assertThat(found.get().getValue()).isEqualTo("Welcome to VOICE");
        assertThat(found.get().isActive()).isTrue();
    }

    @Test
    void findByKey_WithNonExistentKey_ShouldReturnEmpty() {
        Optional<LandingPageContent> found = landingPageContentRepository.findByKey("non.existent.key");

        assertThat(found).isEmpty();
    }

    @Test
    void findByKey_WithMultipleContent_ShouldReturnCorrectOne() {
        entityManager.persist(heroTitle);
        entityManager.persist(heroSubtitle);
        entityManager.persist(aboutSection);
        entityManager.flush();

        Optional<LandingPageContent> foundTitle = landingPageContentRepository.findByKey("hero.title");
        Optional<LandingPageContent> foundSubtitle = landingPageContentRepository.findByKey("hero.subtitle");
        Optional<LandingPageContent> foundAbout = landingPageContentRepository.findByKey("about.section");

        assertThat(foundTitle).isPresent();
        assertThat(foundTitle.get().getValue()).isEqualTo("Welcome to VOICE");

        assertThat(foundSubtitle).isPresent();
        assertThat(foundSubtitle.get().getValue()).contains("Supporting families");

        assertThat(foundAbout).isPresent();
        assertThat(foundAbout.get().getValue()).contains("non-profit organization");
    }

    @Test
    void findByKey_IsCaseSensitive() {
        entityManager.persist(heroTitle);
        entityManager.flush();

        Optional<LandingPageContent> foundLowercase = landingPageContentRepository.findByKey("hero.title");
        Optional<LandingPageContent> foundUppercase = landingPageContentRepository.findByKey("HERO.TITLE");

        assertThat(foundLowercase).isPresent();
        assertThat(foundUppercase).isEmpty();
    }

    @Test
    void save_ShouldPersistContent() {
        LandingPageContent newContent = new LandingPageContent();
        newContent.setKey("footer.text");
        newContent.setValue("© 2026 VOICE. All rights reserved.");
        newContent.setActive(true);

        LandingPageContent saved = landingPageContentRepository.save(newContent);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getKey()).isEqualTo("footer.text");
        assertThat(saved.getValue()).isEqualTo("© 2026 VOICE. All rights reserved.");
    }

    @Test
    void save_WithDuplicateKey_ShouldUpdateExisting() {
        LandingPageContent content = entityManager.persist(heroTitle);
        entityManager.flush();

        Integer contentId = content.getId();

        // Update the existing content
        LandingPageContent foundContent = landingPageContentRepository.findById(contentId).orElseThrow();
        foundContent.setValue("Updated Welcome Message");
        landingPageContentRepository.save(foundContent);
        entityManager.flush();

        LandingPageContent updated = landingPageContentRepository.findById(contentId).orElseThrow();
        assertThat(updated.getValue()).isEqualTo("Updated Welcome Message");
        assertThat(updated.getKey()).isEqualTo("hero.title");
    }

    @Test
    void delete_ShouldRemoveContent() {
        LandingPageContent content = entityManager.persist(heroTitle);
        entityManager.flush();

        Integer contentId = content.getId();
        landingPageContentRepository.delete(content);
        entityManager.flush();

        assertThat(landingPageContentRepository.findById(contentId)).isEmpty();
        assertThat(landingPageContentRepository.findByKey("hero.title")).isEmpty();
    }

    @Test
    void findAll_ShouldReturnAllContent() {
        entityManager.persist(heroTitle);
        entityManager.persist(heroSubtitle);
        entityManager.persist(aboutSection);
        entityManager.flush();

        List<LandingPageContent> all = landingPageContentRepository.findAll();

        assertThat(all).hasSize(3);
        assertThat(all).extracting(LandingPageContent::getKey)
                .containsExactlyInAnyOrder("hero.title", "hero.subtitle", "about.section");
    }

    @Test
    void findById_WithExistingId_ShouldReturnContent() {
        LandingPageContent content = entityManager.persist(heroTitle);
        entityManager.flush();

        Optional<LandingPageContent> found = landingPageContentRepository.findById(content.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getKey()).isEqualTo("hero.title");
    }

    @Test
    void findById_WithNonExistentId_ShouldReturnEmpty() {
        Optional<LandingPageContent> found = landingPageContentRepository.findById(99999);

        assertThat(found).isEmpty();
    }

    @Test
    void save_WithEmptyValue_ShouldPersist() {
        LandingPageContent content = new LandingPageContent();
        content.setKey("placeholder.key");
        content.setValue("");
        content.setActive(false);

        LandingPageContent saved = landingPageContentRepository.save(content);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getValue()).isEmpty();
        assertThat(saved.getKey()).isEqualTo("placeholder.key");
    }

    @Test
    void save_WithLongValue_ShouldPersist() {
        LandingPageContent content = new LandingPageContent();
        content.setKey("long.content");
        content.setValue("This is a very long content that spans multiple lines and contains " +
                "a lot of information about the organization, its mission, vision, and values. " +
                "It may include HTML formatting, special characters, and other rich text elements.");
        content.setActive(true);

        LandingPageContent saved = landingPageContentRepository.save(content);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getValue()).hasSizeGreaterThan(100);
    }

    @Test
    void update_ShouldModifyExistingContent() {
        LandingPageContent content = entityManager.persist(heroSubtitle);
        entityManager.flush();

        Integer contentId = content.getId();

        // Update all fields
        LandingPageContent foundContent = landingPageContentRepository.findById(contentId).orElseThrow();
        foundContent.setValue("New subtitle text");
        foundContent.setActive(false);
        landingPageContentRepository.save(foundContent);
        entityManager.flush();

        LandingPageContent updated = landingPageContentRepository.findById(contentId).orElseThrow();
        assertThat(updated.getValue()).isEqualTo("New subtitle text");
        assertThat(updated.isActive()).isFalse();
        assertThat(updated.getKey()).isEqualTo("hero.subtitle"); // Key should not change
    }

    @Test
    void count_ShouldReturnTotalRecords() {
        entityManager.persist(heroTitle);
        entityManager.persist(heroSubtitle);
        entityManager.flush();

        long count = landingPageContentRepository.count();

        assertThat(count).isEqualTo(2);
    }
}
