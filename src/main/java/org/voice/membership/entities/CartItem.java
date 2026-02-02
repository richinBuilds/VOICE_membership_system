package org.voice.membership.entities;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
/**

 Represents individual items in a user's shopping cart.
 Links cart to specific membership and tracks quantity and pricing.
 Stores unit price and total price for each membership in cart.
 Used during checkout to calculate total membership cost.
 */
@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "cart_items")
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "membership_id", nullable = false)
    private Membership membership;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "unit_price", columnDefinition = "DECIMAL(10, 2)")
    private BigDecimal unitPrice;

    @Column(name = "total_price", columnDefinition = "DECIMAL(10, 2)")
    private BigDecimal totalPrice;
}

