/**
 Data access layer for Membership entity.
 Provides database queries for membership operations.
 Finds active memberships and orders them by display order for UI presentation.
 Used to load membership options during registration and landing page display.
 */
package org.voice.membership.repositories;

import org.voice.membership.entities.Membership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MembershipRepository extends JpaRepository<Membership, Integer> {
    List<Membership> findByActiveTrue();

    List<Membership> findByActiveTrueOrderByDisplayOrderAsc();

    List<Membership> findByIsFree(boolean isFree);
    
    List<Membership> findByNameAndIsFreeTrue(String name);
    
    List<Membership> findByNameAndIsFreeFalse(String name);
}
