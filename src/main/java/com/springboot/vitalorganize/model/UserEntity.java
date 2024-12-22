package com.springboot.vitalorganize.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
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

    @JsonIgnore
    @ManyToMany(mappedBy = "users")
    private List<ChatGroup> chatGroups;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "direct_chat_id")
    private DirectChat directChat; // Für Einzelchats

    @Column(name = "profile_picture_url", length = 1024)
    private String profilePictureUrl;  // Profilbild-URL

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    @JsonIgnore
    private PersonalInformation personalInformation;

    private String twoFactorCode; // Temporärer 2FA-Code
    private LocalDateTime twoFactorExpiry; // Ablaufzeit des Codes

}
