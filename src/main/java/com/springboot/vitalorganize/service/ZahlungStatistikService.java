package com.springboot.vitalorganize.service;

import com.springboot.vitalorganize.dto.ZahlungStatistikRequest;
import com.springboot.vitalorganize.model.*;
import com.springboot.vitalorganize.repository.FundRepository;
import com.springboot.vitalorganize.repository.UserRepository;
import com.springboot.vitalorganize.repository.ZahlungStatistikRepository;
import com.springboot.vitalorganize.service.repositoryhelper.FundRepositoryService;
import com.springboot.vitalorganize.service.repositoryhelper.UserRepositoryService;
import com.springboot.vitalorganize.service.repositoryhelper.ZahlungsStatistikRepositoryService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.format.DateTimeFormatter;

import java.time.LocalDate;
import java.util.List;

@Service
@AllArgsConstructor
public class ZahlungStatistikService {


    private final FundRepositoryService fundRepositoryService;
    private final UserRepositoryService userRepositoryService;
    private ZahlungsStatistikRepositoryService zahlungsStatistikRepositoryService;

    public List<ZahlungStatistik> getAllZahlungStatistiken() {
        return zahlungsStatistikRepositoryService.findAll();
    }

    public ZahlungStatistik getZahlungStatistikById(Long id) {
        return zahlungsStatistikRepositoryService.findById(id);
    }


    // Delete
    public void deleteZahlungStatistik(Long id) {
        if (zahlungsStatistikRepositoryService.findById(id) != null) {
            zahlungsStatistikRepositoryService.deleteById(id);

        }
    }

    public ZahlungStatistik createZahlungStatistikWithMinimalInput(Long fundId, String startDate, String endDate) {
        // Fund abrufen
        FundEntity fund = fundRepositoryService.findFundById(fundId);

        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);

        // Zahlungen im Zeitraum filtern
        List<Payment> paymentsInPeriod = fund.getPayments().stream()
                .filter(payment -> {
                    LocalDate paymentDate = payment.getDate().toLocalDate();
                    return !paymentDate.isBefore(start) && !paymentDate.isAfter(end);
                })
                .toList();

        // Berechnungen durchführen
        double totalAmount = paymentsInPeriod.stream().mapToDouble(Payment::getAmount).sum();
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

        return zahlungsStatistikRepositoryService.saveStatistic(zahlungStatistik);
    }


    public List<FundEntity> getAllFunds(String accessToken) {
        System.out.println(accessToken);
        // Überprüfe, ob der User anhand des Tokens existiert
        UserEntity user = userRepositoryService.findByToken(accessToken);
        if (user == null) {
            throw new IllegalArgumentException("Invalid access token or user not found.");
        }

        // Liste der Funds abrufen, die dem User zugeordnet sind
        List<FundEntity> funds = fundRepositoryService.findALl();
        System.out.println(funds);
        funds = funds.stream()
                .filter(f -> f.getUsers().contains(user))  // Filtere die Funds, die mit diesem User verknüpft sind
                .toList();

        return funds;
    }

    public ZahlungStatistik createOrUpdateZahlungStatistik(Long id, ZahlungStatistikRequest request) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;  // Hier kannst du das Format anpassen
        LocalDate startDate = LocalDate.parse(request.getStartDate(), formatter);
        LocalDate endDate = LocalDate.parse(request.getEndDate(), formatter);

        ZahlungStatistik existing = zahlungsStatistikRepositoryService.findById(id);

        if (existing != null) {
            existing.setFund(request.getFundId());
            existing.setStartDate(startDate);
            existing.setEndDate(endDate);
            return zahlungsStatistikRepositoryService.saveStatistic(existing);
        } else {
            ZahlungStatistik newStatistik = new ZahlungStatistik();
            newStatistik.setFund(request.getFundId());
            newStatistik.setStartDate(startDate);
            newStatistik.setEndDate(endDate);
            return zahlungsStatistikRepositoryService.saveStatistic(newStatistik);
        }
    }

    public String extractAccessToken(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7); // Entfernt "Bearer " und gibt den Token zurück
        } else {
            throw new IllegalArgumentException("Authorization header must be in the form 'Bearer <token>'");
        }
    }
}
