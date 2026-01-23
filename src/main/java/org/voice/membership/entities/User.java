package org.voice.membership.entities;

import jakarta.persistence.*;
import lombok.*;
import java.util.Date;

@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String name;
    private String email;
    private String password;
    private String phone;
    private String address;
    @Column(name = "postal_code")
    private String postalCode;
    private String role;
    private Date creation;
}