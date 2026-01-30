/**

 Data access layer for CartItem entity.
 Provides database operations for cart items.
 Finds cart items by cart and supports bulk deletion when cart is cleared.
 Used during checkout process to manage membership items in cart.
 */
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
    
    // For enforcing single membership selection
    void deleteByCartId(Integer cartId);
    List<CartItem> findByCartId(Integer cartId);
}
