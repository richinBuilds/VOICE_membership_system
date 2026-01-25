package org.voice.membership.repositories;

import org.voice.membership.entities.Cart;
import org.voice.membership.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Integer> {
    Optional<Cart> findByUser(User user);
    Optional<Cart> findByUserId(int userId);
}
