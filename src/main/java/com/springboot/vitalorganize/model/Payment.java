package com.springboot.vitalorganize.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime date; // Wann

    @Column(nullable = false)
    private Double amount; // Wie viel

    @Column(nullable = false)
    private String reason; // Warum

    @Column(nullable = false)
    private String currency;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = true) // Verknüpfung mit UserEntity
    private UserEntity user; // Wer

    @Enumerated(EnumType.STRING) // Speichert den Enum-Wert als String in der Datenbank
    @Column(nullable = false)
    private PaymentType type;

    @Column(nullable = false)
    private double balance;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL) // Jede Zahlung gehört zu einem Fund
    @JoinColumn(name = "fund_id", nullable = false) // Verknüpfung zum Fund
    private FundEntity fund; // Fund, in den die Zahlung getätigt wurde

}
