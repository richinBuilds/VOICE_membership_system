/**

 Data access layer for Cart entity.
 Provides database queries for shopping cart operations.
 Finds user carts by user object or user ID for checkout and cart management.
 Used during registration Step 4 (checkout) and payment processing.
 */
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

