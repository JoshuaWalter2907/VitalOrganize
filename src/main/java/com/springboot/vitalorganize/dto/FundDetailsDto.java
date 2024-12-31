package com.springboot.vitalorganize.dto;

import com.springboot.vitalorganize.model.FundEntity;
import com.springboot.vitalorganize.model.Zahlung;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class FundDetailsDto {
    private List<FundEntity> funds;          // Liste aller Funds
    private FundEntity myFund;              // Aktuell ausgewählter Fund
    private List<Zahlung> filteredPayments; // Gefilterte Zahlungen
    private Double balance;                 // Aktueller PayPal-Kontostand
    private Boolean error;                  // Fehlerindikator
}
