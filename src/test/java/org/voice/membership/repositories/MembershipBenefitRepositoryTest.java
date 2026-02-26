package org.voice.membership.repositories;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.voice.membership.entities.MembershipBenefit;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for MembershipBenefitRepository
 * Tests database operations for membership benefits
 */
@DataJpaTest
@ActiveProfiles("test")
class MembershipBenefitRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private MembershipBenefitRepository membershipBenefitRepository;

    private MembershipBenefit benefit1;
    private MembershipBenefit benefit2;
    private MembershipBenefit benefit3;
    private MembershipBenefit inactiveBenefit;

    @BeforeEach
    void setUp() {
        benefit1 = new MembershipBenefit();
        benefit1.setTitle("Access to Events");
        benefit1.setDescription("Free access to all VOICE events");
        benefit1.setIcon("fa-calendar");
        benefit1.setActive(true);
        benefit1.setDisplayOrder(1);

        benefit2 = new MembershipBenefit();
        benefit2.setTitle("Newsletter Subscription");
        benefit2.setDescription("Monthly newsletter with updates");
        benefit2.setIcon("fa-envelope");
        benefit2.setActive(true);
        benefit2.setDisplayOrder(2);

        benefit3 = new MembershipBenefit();
        benefit3.setTitle("Community Support");
        benefit3.setDescription("Access to community forums");
        benefit3.setIcon("fa-users");
        benefit3.setActive(true);
        benefit3.setDisplayOrder(3);

        inactiveBenefit = new MembershipBenefit();
        inactiveBenefit.setTitle("Inactive Benefit");
        inactiveBenefit.setDescription("This benefit is not active");
        inactiveBenefit.setIcon("fa-times");
        inactiveBenefit.setActive(false);
        inactiveBenefit.setDisplayOrder(4);
    }

    @Test
    void findByActiveTrue_WithActiveAndInactiveBenefits_ShouldReturnOnlyActive() {
        entityManager.persist(benefit1);
        entityManager.persist(benefit2);
        entityManager.persist(inactiveBenefit);
        entityManager.flush();

        List<MembershipBenefit> found = membershipBenefitRepository.findByActiveTrue();

        assertThat(found).hasSize(2);
        assertThat(found).extracting(MembershipBenefit::getTitle)
                .containsExactlyInAnyOrder("Access to Events", "Newsletter Subscription");
        assertThat(found).noneMatch(b -> !b.isActive());
    }

    @Test
    void findByActiveTrue_WithOnlyInactiveBenefits_ShouldReturnEmptyList() {
        entityManager.persist(inactiveBenefit);
        entityManager.flush();

        List<MembershipBenefit> found = membershipBenefitRepository.findByActiveTrue();

        assertThat(found).isEmpty();
    }

    @Test
    void findByActiveTrue_WithNoBenefits_ShouldReturnEmptyList() {
        List<MembershipBenefit> found = membershipBenefitRepository.findByActiveTrue();

        assertThat(found).isEmpty();
    }

    @Test
    void findByActiveTrueOrderByDisplayOrderAsc_ShouldReturnBenefitsInOrder() {
        entityManager.persist(benefit3);
        entityManager.persist(benefit1);
        entityManager.persist(benefit2);
        entityManager.persist(inactiveBenefit);
        entityManager.flush();

        List<MembershipBenefit> found = membershipBenefitRepository.findByActiveTrueOrderByDisplayOrderAsc();

        assertThat(found).hasSize(3);
        assertThat(found).extracting(MembershipBenefit::getTitle)
                .containsExactly("Access to Events", "Newsletter Subscription", "Community Support");
        assertThat(found).extracting(MembershipBenefit::getDisplayOrder)
                .containsExactly(1, 2, 3);
    }

    @Test
    void findByActiveTrueOrderByDisplayOrderAsc_WithUnsortedData_ShouldSortCorrectly() {
        // Insert in random order
        entityManager.persist(benefit2); // display order 2
        entityManager.persist(benefit3); // display order 3
        entityManager.persist(benefit1); // display order 1
        entityManager.flush();

        List<MembershipBenefit> found = membershipBenefitRepository.findByActiveTrueOrderByDisplayOrderAsc();

        assertThat(found).hasSize(3);
        assertThat(found.get(0).getDisplayOrder()).isEqualTo(1);
        assertThat(found.get(1).getDisplayOrder()).isEqualTo(2);
        assertThat(found.get(2).getDisplayOrder()).isEqualTo(3);
    }

    @Test
    void findByActiveTrueOrderByDisplayOrderAsc_WithOnlyInactive_ShouldReturnEmpty() {
        entityManager.persist(inactiveBenefit);
        entityManager.flush();

        List<MembershipBenefit> found = membershipBenefitRepository.findByActiveTrueOrderByDisplayOrderAsc();

        assertThat(found).isEmpty();
    }

    @Test
    void save_ShouldPersistBenefit() {
        MembershipBenefit newBenefit = new MembershipBenefit();
        newBenefit.setTitle("New Benefit");
        newBenefit.setDescription("A newly created benefit");
        newBenefit.setIcon("fa-star");
        newBenefit.setActive(true);
        newBenefit.setDisplayOrder(5);

        MembershipBenefit saved = membershipBenefitRepository.save(newBenefit);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTitle()).isEqualTo("New Benefit");
        assertThat(saved.isActive()).isTrue();
        assertThat(saved.getDisplayOrder()).isEqualTo(5);
    }

    @Test
    void update_ShouldModifyBenefit() {
        MembershipBenefit benefit = entityManager.persist(benefit1);
        entityManager.flush();

        Integer benefitId = benefit.getId();

        // Update the benefit
        MembershipBenefit foundBenefit = membershipBenefitRepository.findById(benefitId).orElseThrow();
        foundBenefit.setTitle("Updated Event Access");
        foundBenefit.setDescription("Updated description");
        foundBenefit.setActive(false);
        membershipBenefitRepository.save(foundBenefit);
        entityManager.flush();

        MembershipBenefit updated = membershipBenefitRepository.findById(benefitId).orElseThrow();
        assertThat(updated.getTitle()).isEqualTo("Updated Event Access");
        assertThat(updated.getDescription()).isEqualTo("Updated description");
        assertThat(updated.isActive()).isFalse();
    }

    @Test
    void delete_ShouldRemoveBenefit() {
        MembershipBenefit benefit = entityManager.persist(benefit1);
        entityManager.flush();

        Integer benefitId = benefit.getId();
        membershipBenefitRepository.delete(benefit);
        entityManager.flush();

        assertThat(membershipBenefitRepository.findById(benefitId)).isEmpty();
    }

    @Test
    void findByActiveTrueOrderByDisplayOrderAsc_WithSameDisplayOrder_ShouldReturnAll() {
        // Create benefits with same display order
        MembershipBenefit benefitA = new MembershipBenefit();
        benefitA.setTitle("Benefit A");
        benefitA.setDescription("Description A");
        benefitA.setIcon("fa-a");
        benefitA.setActive(true);
        benefitA.setDisplayOrder(1);

        MembershipBenefit benefitB = new MembershipBenefit();
        benefitB.setTitle("Benefit B");
        benefitB.setDescription("Description B");
        benefitB.setIcon("fa-b");
        benefitB.setActive(true);
        benefitB.setDisplayOrder(1);

        entityManager.persist(benefitA);
        entityManager.persist(benefitB);
        entityManager.flush();

        List<MembershipBenefit> found = membershipBenefitRepository.findByActiveTrueOrderByDisplayOrderAsc();

        assertThat(found).hasSize(2);
        assertThat(found).allMatch(b -> b.getDisplayOrder() == 1);
    }

    @Test
    void findAll_ShouldReturnAllBenefits() {
        entityManager.persist(benefit1);
        entityManager.persist(benefit2);
        entityManager.persist(inactiveBenefit);
        entityManager.flush();

        List<MembershipBenefit> all = membershipBenefitRepository.findAll();

        assertThat(all).hasSize(3);
        assertThat(all).extracting(MembershipBenefit::isActive)
                .containsExactlyInAnyOrder(true, true, false);
    }
}
