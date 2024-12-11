package com.springboot.vitalorganize.model;

import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.lang.annotation.Documented;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@Entity
@Table(name = "users")
public class UserEntity {

    // Getter und Setter
    @Setter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    private String email;
    @Setter
    private String username;  // Verwende weiterhin die E-Mail als Benutzername
    @Setter
    private String password;
    @Setter
    private String role;
    @Setter
    private LocalDate birthday;

    @Setter
    @Column(nullable = false)
    private String provider; // Der OAuth2-Provider, z.B. "google", "discord"

    @Setter
    @Column(name = "is_public", nullable = false)
    private boolean isPublic;


}
