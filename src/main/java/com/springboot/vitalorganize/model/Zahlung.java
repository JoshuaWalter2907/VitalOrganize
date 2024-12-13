package com.springboot.vitalorganize.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "payments")
public class Zahlung {

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
    @JoinColumn(name = "user_id", nullable = false) // Verknüpfung mit UserEntity
    private UserEntity user; // Wer

    @Enumerated(EnumType.STRING) // Speichert den Enum-Wert als String in der Datenbank
    @Column(nullable = false)
    private PaymentType type;

    @Column(nullable = false)
    private double balance;

}
