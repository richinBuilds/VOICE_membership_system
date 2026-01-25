package org.voice.membership.repositories;

import org.voice.membership.entities.Child;
import org.voice.membership.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChildRepository extends JpaRepository<Child, Integer> {
    List<Child> findByUser(User user);
    List<Child> findByUserId(int userId);
}
