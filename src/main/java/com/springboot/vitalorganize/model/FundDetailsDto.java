package com.springboot.vitalorganize.model;

import com.springboot.vitalorganize.entity.FundEntity;
import com.springboot.vitalorganize.entity.Payment;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class FundDetailsDto {
    private List<FundEntity> funds;          // Liste aller Funds
    private FundEntity myFund;              // Aktuell ausgewählter Fund
    private List<Payment> filteredPayments; // Gefilterte Zahlungen
    private Double balance;                 // Aktueller PayPal-Kontostand
    private Boolean error;                  // Fehlerindikator
}
