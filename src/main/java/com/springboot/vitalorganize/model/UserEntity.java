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

    @Column(name = "two_factor_code")
    private String twoFactorCode; // Temporärer 2FA-Code

    @Column(name = "two_factor_expiry")
    private LocalDateTime twoFactorExpiry; // Ablaufzeit des Codes

    @Column(name = "sendtoameil", length = 1024, nullable = true)
    private String sendtoEmail;

    @JsonIgnore
    @ManyToMany
    @JoinTable(
            name = "user_friends",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "friend_id")
    )
    private List<UserEntity> friends = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "receiver", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FriendRequest> receivedFriendRequests = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "sender", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FriendRequest> sentFriendRequests = new ArrayList<>();

    @JsonIgnore
    @ManyToMany
    @JoinTable(
            name = "user_blocked_users",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "blocked_user_id")
    )
    private List<UserEntity> blockedUsers = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SubscriptionEntity> subscriptions;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserEntity that = (UserEntity) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    public SubscriptionEntity getLatestSubscription() {
        if (subscriptions == null || subscriptions.isEmpty()) {
            return null; // Keine Subscription vorhanden
        }
        // Sortiere nach einer Eigenschaft, z. B. Startzeit oder ID
        return subscriptions.stream()
                .max((s1, s2) -> s1.getStartTime().compareTo(s2.getStartTime())) // Nach Startzeit sortieren
                .orElse(null);
    }

}
