package com.springboot.vitalorganize.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@Entity
@Table(name = "subscriptions")
public class SubscriptionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String subscriptionId;  // PayPal Subscription ID (z.B. I-R0222XG1FH59)
    private String payerId;         // PayPal Payer ID
    private String planId;          // PayPal Plan ID (z.B. P-1DP07006BV376124WM5XEB6Q)
    private String status;          // Der Status der Subscription (ACTIVE, SUSPENDED, CANCELLED)
    private String startTime;       // Startzeit der Subscription
    private String nextBillingTime; // Nächster Zahlungszeitpunkt
    private LocalDateTime endTime;    // Ablaufdatum der Subscription (unabhängig vom Status)

    // Verknüpfung mit UserEntity (One-to-One Beziehung)
    @ManyToOne
    @JsonIgnore
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;


    public boolean hasActivePrivileges() {
        return endTime != null && LocalDateTime.now().isBefore(endTime);
    }

}
