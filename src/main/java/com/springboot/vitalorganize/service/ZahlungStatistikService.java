package com.springboot.vitalorganize.service;

import com.springboot.vitalorganize.model.*;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class ZahlungStatistikService {


    private ZahlungStatistikRepository zahlungStatistikRepository;
    private FundRepository fundRepository;
    private UserRepository userRepository;

    // Create
    public ZahlungStatistik createZahlungStatistik(ZahlungStatistik zahlungStatistik) {
        return zahlungStatistikRepository.save(zahlungStatistik);
    }

    // Read all
    public List<ZahlungStatistik> getAllZahlungStatistiken() {
        return zahlungStatistikRepository.findAll();
    }

    // Read by ID
    public ZahlungStatistik getZahlungStatistikById(Long id) {
        return zahlungStatistikRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("ZahlungStatistik not found with id " + id));
    }

    // Update
    public ZahlungStatistik updateZahlungStatistik(Long id, Long fundId, String startDate, String endDate) {
        // Zuerst die bestehende Statistik finden
        ZahlungStatistik existing = zahlungStatistikRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("ZahlungStatistik not found with id " + id));

        // Fund abrufen und neue Datumswerte setzen
        FundEntity fund = fundRepository.findById(fundId)
                .orElseThrow(() -> new RuntimeException("Fund not found with id " + fundId));

        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);

        // Zahlungen im Zeitraum filtern
        List<Zahlung> paymentsInPeriod = fund.getPayments().stream()
                .filter(payment -> {
                    LocalDate paymentDate = payment.getDate().toLocalDate();
                    return !paymentDate.isBefore(start) && !paymentDate.isAfter(end);
                })
                .toList();

        // Berechnungen durchführen
        double totalAmount = paymentsInPeriod.stream().mapToDouble(Zahlung::getAmount).sum();
        long paymentCount = paymentsInPeriod.size();
        double averageAmount = paymentCount > 0 ? totalAmount / paymentCount : 0.0;

        // Werte in der vorhandenen ZahlungStatistik aktualisieren
        existing.setFund(fund);
        existing.setStartDate(start);
        existing.setEndDate(end);
        existing.setTotalAmount(totalAmount);
        existing.setAverageAmount(averageAmount);
        existing.setPaymentCount(paymentCount);

        if (!paymentsInPeriod.isEmpty()) {
            existing.setCurrency(paymentsInPeriod.get(0).getCurrency());
        } else {
            existing.setCurrency("EUR"); // Standardwert
        }

        return zahlungStatistikRepository.save(existing);
    }

    // Delete
    public void deleteZahlungStatistik(Long id) {
        if (!zahlungStatistikRepository.existsById(id)) {
            throw new RuntimeException("ZahlungStatistik not found with id " + id);
        }
        zahlungStatistikRepository.deleteById(id);
    }

    public ZahlungStatistik createZahlungStatistikWithMinimalInput(Long fundId, String startDate, String endDate) {
        // Fund abrufen
        FundEntity fund = fundRepository.findById(fundId)
                .orElseThrow(() -> new RuntimeException("Fund not found with id " + fundId));

        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);

        // Zahlungen im Zeitraum filtern
        List<Zahlung> paymentsInPeriod = fund.getPayments().stream()
                .filter(payment -> {
                    LocalDate paymentDate = payment.getDate().toLocalDate();
                    return !paymentDate.isBefore(start) && !paymentDate.isAfter(end);
                })
                .toList();

        // Berechnungen durchführen
        double totalAmount = paymentsInPeriod.stream().mapToDouble(Zahlung::getAmount).sum();
        long paymentCount = paymentsInPeriod.size();
        double averageAmount = paymentCount > 0 ? totalAmount / paymentCount : 0.0;

        // ZahlungStatistik erstellen
        ZahlungStatistik zahlungStatistik = new ZahlungStatistik();
        zahlungStatistik.setFund(fund);
        zahlungStatistik.setStartDate(start);
        zahlungStatistik.setEndDate(end);
        zahlungStatistik.setTotalAmount(totalAmount);
        zahlungStatistik.setAverageAmount(averageAmount);
        zahlungStatistik.setPaymentCount(paymentCount);

        // Währung von der ersten Zahlung übernehmen, falls vorhanden
        if (!paymentsInPeriod.isEmpty()) {
            zahlungStatistik.setCurrency(paymentsInPeriod.get(0).getCurrency());
        } else {
            zahlungStatistik.setCurrency("EUR"); // Standardwert
        }

        return zahlungStatistikRepository.save(zahlungStatistik);
    }


    public List<FundEntity> getAllFunds(String accessToken) {
        System.out.println(accessToken);
        // Überprüfe, ob der User anhand des Tokens existiert
        UserEntity user = userRepository.findByToken(accessToken);
        if (user == null) {
            throw new IllegalArgumentException("Invalid access token or user not found.");
        }

        // Liste der Funds abrufen, die dem User zugeordnet sind
        List<FundEntity> funds = fundRepository.findAll();
        System.out.println(funds);
        funds = funds.stream()
                .filter(f -> f.getUsers().contains(user))  // Filtere die Funds, die mit diesem User verknüpft sind
                .toList();

        return funds;
    }

    private String extractAccessToken(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7); // Entfernt "Bearer " und gibt den Token zurück
        } else {
            throw new IllegalArgumentException("Authorization header must be in the form 'Bearer <token>'");
        }
    }
}
