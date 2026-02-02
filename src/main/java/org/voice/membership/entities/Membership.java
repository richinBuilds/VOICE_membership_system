package org.voice.membership.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

/**
 * 
 * Represents different membership plan options available to users.
 * Stores membership details: name, description, price, features, and active
 * status.
 * Two main memberships: Free and Premium ($20/year).
 * Used during registration (Step 3) and displayed on landing page.
 */
@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "membership_options")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class Membership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "DECIMAL(10, 2)")
    private BigDecimal price;

    @Column(columnDefinition = "TEXT")
    private String features;

    @Column(name = "is_free")
    private boolean isFree;

    @Column(name = "display_order")
    private int displayOrder;

    private boolean active;
}
