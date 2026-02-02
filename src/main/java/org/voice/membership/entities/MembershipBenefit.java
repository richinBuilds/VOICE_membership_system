package org.voice.membership.entities;

import jakarta.persistence.*;
import lombok.*;
/**

 Represents individual benefits associated with membership plans.
 Stores benefit information: title, description, icon, and display order.
 Example benefits: Community Network, Exclusive Content, Career Opportunities, etc.
 Displayed on landing page to highlight membership features.
 */
@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "membership_benefits")
public class MembershipBenefit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String icon;

    @Column(name = "display_order")
    private int displayOrder;

    private boolean active;
}
