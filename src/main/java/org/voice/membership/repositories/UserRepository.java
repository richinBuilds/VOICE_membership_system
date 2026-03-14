package org.voice.membership.repositories;

import org.voice.membership.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;

/**
 * Spring Data repository for accessing and querying User entities.
 * Provides methods for looking up users by email, including case-insensitive
 * search.
 */
public interface UserRepository extends JpaRepository<User, Integer> {
       User findByEmail(String email);

       User findByEmailIgnoreCase(String email);

       List<User> findAllByEmailIgnoreCase(String email);

       /**
        * Find new paid members registered between two dates.
        * A paid member is defined as a user with a non-free membership and paid=true.
        */
       @Query("SELECT u FROM User u WHERE u.paid = true AND u.membership IS NOT NULL " +
                     "AND u.membership.isFree = false AND u.creation BETWEEN :startDate AND :endDate")
       List<User> findNewPaidMembersBetweenDates(@Param("startDate") Date startDate, @Param("endDate") Date endDate);

       /**
        * Find all new members (both paid and free) registered between two dates.
        */
       @Query("SELECT u FROM User u WHERE u.creation BETWEEN :startDate AND :endDate ORDER BY u.creation DESC")
       List<User> findNewMembersBetweenDates(@Param("startDate") Date startDate, @Param("endDate") Date endDate);
}
