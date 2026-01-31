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

    @Test
    void findByActiveTrue_ShouldReturnOnlyActiveMemberships() {
        // Given
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

        // When
        List<Membership> activeMemberships = membershipRepository.findByActiveTrue();

        // Then
        assertThat(activeMemberships).hasSize(2);
        assertThat(activeMemberships).extracting(Membership::getName)
                .containsExactlyInAnyOrder("Free", "Premium");
    }

    @Test
    void save_ShouldPersistMembership() {
        // When
        Membership saved = membershipRepository.save(freeMembership);

        // Then
        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isGreaterThan(0);
        assertThat(entityManager.find(Membership.class, saved.getId())).isEqualTo(saved);
    }

    @Test
    void count_ShouldReturnTotalMemberships() {
        // Given
        entityManager.persist(freeMembership);
        entityManager.persist(premiumMembership);
        entityManager.flush();

        // When
        long count = membershipRepository.count();

        // Then
        assertThat(count).isEqualTo(2);
    }

    @Test
    void findById_WithExistingId_ShouldReturnMembership() {
        // Given
        entityManager.persist(freeMembership);
        entityManager.flush();
        int membershipId = freeMembership.getId();

        // When
        Membership found = membershipRepository.findById(membershipId).orElse(null);

        // Then
        assertThat(found).isNotNull();
        assertThat(found.getName()).isEqualTo("Free");
        assertThat(found.isFree()).isTrue();
    }
}
