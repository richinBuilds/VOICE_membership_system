package org.voice.membership.entities;

import jakarta.persistence.*;
import lombok.*;

/**
 * JPA entity that stores configurable text content for the landing page.
 * Key/value pairs allow the tagline and other messages to be managed in DB.
 */
@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "landing_page_content")
public class LandingPageContent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "`key`", nullable = false, unique = true)
    private String key;

    @Column(name = "`value`", columnDefinition = "LONGTEXT")
    private String value;

    private boolean active;
}

