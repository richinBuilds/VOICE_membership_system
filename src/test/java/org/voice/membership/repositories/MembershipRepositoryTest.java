package org.voice.membership.repositories;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.voice.membership.entities.Membership;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for MembershipRepository
 * Tests membership data access operations
 */
@DataJpaTest
@ActiveProfiles("test")
class MembershipRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private MembershipRepository membershipRepository;

    private Membership freeMembership;
    private Membership premiumMembership;

    @BeforeEach
    void setUp() {
        freeMembership = Membership.builder()
                .name("Free")
                .description("Basic membership")
                .price(BigDecimal.ZERO)
                .isFree(true)
                .displayOrder(1)
                .active(true)
                .build();

        premiumMembership = Membership.builder()
                .name("Premium")
                .description("Premium membership")
                .price(new BigDecimal("20.00"))
                .isFree(false)
                .displayOrder(2)
                .active(true)
                .build();
    }

    // Test 1: Find only active memberships (exclude inactive ones)
    @Test
    void findByActiveTrue_ShouldReturnOnlyActiveMemberships() {
        Membership inactiveMembership = Membership.builder()
                .name("Inactive")
                .description("Inactive membership")
                .price(new BigDecimal("10.00"))
                .isFree(false)
                .displayOrder(3)
                .active(false)
                .build();

        entityManager.persist(freeMembership);
        entityManager.persist(premiumMembership);
        entityManager.persist(inactiveMembership);
        entityManager.flush();

        List<Membership> activeMemberships = membershipRepository.findByActiveTrue();

        assertThat(activeMemberships).hasSize(2);
        assertThat(activeMemberships).extracting(Membership::getName)
                .containsExactlyInAnyOrder("Free", "Premium");
    }

    // Test 2: Save membership to database
    @Test
    void save_ShouldPersistMembership() {
        Membership saved = membershipRepository.save(freeMembership);

        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isGreaterThan(0);
        assertThat(entityManager.find(Membership.class, saved.getId())).isEqualTo(saved);
    }

    // Test 3: Count total number of memberships in database
    @Test
    void count_ShouldReturnTotalMemberships() {
        entityManager.persist(freeMembership);
        entityManager.persist(premiumMembership);
        entityManager.flush();

        long count = membershipRepository.count();

        assertThat(count).isEqualTo(2);
    }

    // Test 4: Find membership by ID from database
    @Test
    void findById_WithExistingId_ShouldReturnMembership() {
        entityManager.persist(freeMembership);
        entityManager.flush();
        int membershipId = freeMembership.getId();

        Membership found = membershipRepository.findById(membershipId).orElse(null);

        assertThat(found).isNotNull();
        assertThat(found.getName()).isEqualTo("Free");
        assertThat(found.isFree()).isTrue();
    }
}
