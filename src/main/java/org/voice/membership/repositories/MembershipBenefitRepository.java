package org.voice.membership.repositories;

import org.voice.membership.entities.MembershipBenefit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
/**
 * Spring Data repository for MembershipBenefit entities.
 * Exposes queries to load active membership benefits in display order.
 */
public interface MembershipBenefitRepository extends JpaRepository<MembershipBenefit, Integer> {
    List<MembershipBenefit> findByActiveTrue();

    List<MembershipBenefit> findByActiveTrueOrderByDisplayOrderAsc();
}
