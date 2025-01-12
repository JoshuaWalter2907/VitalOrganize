package com.springboot.vitalorganize.entity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@Table(name = "payment_statistics")
public class ZahlungStatistik {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE, CascadeType.PERSIST})
    @JoinColumn(name = "fund_id", nullable = false) // Verknüpfung zum Fund
    @JsonIgnore
    private FundEntity fund;

    @Column(nullable = false)
    private Double totalAmount; // Gesamtsumme der Zahlungen in diesem Zeitraum

    @Column(nullable = false)
    private Double averageAmount; // Durchschnittsbetrag der Zahlungen in diesem Zeitraum

    @Column(nullable = false)
    private Long paymentCount; // Anzahl der Zahlungen im angegebenen Zeitraum

    @Column(nullable = false)
    private LocalDate startDate; // Startdatum des Zeitraums

    @Column(nullable = false)
    private LocalDate endDate; // Enddatum des Zeitraums

    @Column(nullable = false)
    private String currency; // Währung der Zahlungen

    // Optional: Zusatzinformationen, z.B. für spezifische User
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true) // Verknüpfung mit UserEntity
    private UserEntity user; // Benutzer, für den die Statistik gilt (optional)
}
