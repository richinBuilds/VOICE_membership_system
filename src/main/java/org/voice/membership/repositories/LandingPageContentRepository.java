package org.voice.membership.repositories;

import org.voice.membership.entities.LandingPageContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
/**
 * Spring Data repository for LandingPageContent entries.
 * Allows lookup of landing page text content by a unique key.
 */
public interface LandingPageContentRepository extends JpaRepository<LandingPageContent, Integer> {
    Optional<LandingPageContent> findByKey(String key);
}
