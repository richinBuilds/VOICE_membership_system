package org.voice.membership.repositories;

import org.voice.membership.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data repository for accessing and querying User entities.
 * Provides methods for looking up users by email, including case-insensitive search.
 */
public interface UserRepository extends JpaRepository<User, Integer> {
    User findByEmail(String email);

    User findByEmailIgnoreCase(String email);

    java.util.List<User> findAllByEmailIgnoreCase(String email);
}
