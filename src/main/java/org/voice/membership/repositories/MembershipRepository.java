package org.voice.membership.repositories;

import org.voice.membership.entities.Membership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MembershipRepository extends JpaRepository<Membership, Integer> {
    List<Membership> findByActiveTrue();

    List<Membership> findByActiveTrueOrderByDisplayOrderAsc();
}