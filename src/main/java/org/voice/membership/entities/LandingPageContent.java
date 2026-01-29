package org.voice.membership.entities;

import jakarta.persistence.*;
import lombok.*;

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
