package org.voice.membership.repositories;

import org.voice.membership.entities.CartItem;
import org.voice.membership.entities.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Integer> {
    List<CartItem> findByCart(Cart cart);
    void deleteByCart(Cart cart);
}
