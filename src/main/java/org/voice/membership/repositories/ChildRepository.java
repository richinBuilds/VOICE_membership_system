package org.voice.membership.repositories;

import org.voice.membership.entities.Child;
import org.voice.membership.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
/**
 * Spring Data repository for Child entities linked to a User.
 * Supports queries to load children for a given user or user id.
 */
public interface ChildRepository extends JpaRepository<Child, Integer> {
    List<Child> findByUser(User user);
    List<Child> findByUserId(int userId);
}

