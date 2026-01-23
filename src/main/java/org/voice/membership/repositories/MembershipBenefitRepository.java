package org.voice.membership.repositories;

import org.voice.membership.entities.MembershipBenefit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MembershipBenefitRepository extends JpaRepository<MembershipBenefit, Integer> {
    List<MembershipBenefit> findByActiveTrue();

    List<MembershipBenefit> findByActiveTrueOrderByDisplayOrderAsc();
}