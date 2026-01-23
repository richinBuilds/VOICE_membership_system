package org.voice.membership.entities;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "membership_options")
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