package com.springboot.vitalorganize.entity.Fund_Payments;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.springboot.vitalorganize.entity.Profile_User.UserEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "payments")
public class PaymentEntity {

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
    @JsonIgnore
    @JoinColumn(name = "user_id", nullable = true) // Verknüpfung mit UserEntity
    private UserEntity user; // Wer

    @Enumerated(EnumType.STRING) // Speichert den Enum-Wert als String in der Datenbank
    @Column(nullable = false)
    private PaymentTypeEnumeration type;

    @Column(nullable = false)
    private double balance;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL) // Jede Zahlung gehört zu einem Fund
    @JoinColumn(name = "fund_id", nullable = false) // Verknüpfung zum Fund
    @JsonIgnore
    private FundEntity fund; // Fund, in den die Zahlung getätigt wurde

}
