package org.voice.membership.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;
import java.util.Date;
/**
Represents a child record associated with a user account.
 Stores child personal information: name, age, date of birth, hearing loss type.
 Also captures equipment type, siblings names, and chapter location.
 Users can add multiple children to their account on the dashboard.
 */

@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "children")
public class Child {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false)
    private String name;

    private Integer age;

    @Column(name = "date_of_birth")
    @Temporal(TemporalType.DATE)
    private Date dateOfBirth;

    @Column(name = "hearing_loss_type")
    private String hearingLossType;

    @Column(name = "equipment_type")
    private String equipmentType;

    @Column(name = "siblings_names")
    private String siblingsNames;

    @Column(name = "chapter_location")
    private String chapterLocation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonBackReference
    private User user;
}

