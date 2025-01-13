package com.springboot.vitalorganize.service;

import com.springboot.vitalorganize.entity.*;
import com.springboot.vitalorganize.model.StatisticsDTO;
import com.springboot.vitalorganize.service.repositoryhelper.FundRepositoryService;
import com.springboot.vitalorganize.service.repositoryhelper.UserRepositoryService;
import com.springboot.vitalorganize.service.repositoryhelper.ZahlungsStatistikRepositoryService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.format.DateTimeFormatter;

import java.time.LocalDate;
import java.util.List;

/**
 * Service-Klasse für die Verwaltung von Zahlungstatistiken.
 * Diese Klasse stellt Methoden zum Abrufen, Erstellen, Bearbeiten und Löschen von Zahlungstatistiken bereit.
 */
@Service
@AllArgsConstructor
public class ZahlungStatistikService {

    // Abhängigkeiten: Repository-Services für Fonds, Benutzer und Zahlungstatistiken
    private final FundRepositoryService fundRepositoryService;
    private final UserRepositoryService userRepositoryService;
    private ZahlungsStatistikRepositoryService zahlungsStatistikRepositoryService;

    /**
     * Ruft alle Zahlungstatistiken ab.
     *
     * @return Eine Liste aller Zahlungstatistiken
     */
    public List<ZahlungStatistik> getAllZahlungStatistiken(StatisticsDTO statisticDTO) {
        return zahlungsStatistikRepositoryService.findAllByFundId(fundRepositoryService.findFundById(statisticDTO.getFundId()));
    }

    /**
     * Ruft eine Zahlungstatistik basierend auf der ID ab.
     *
     * @return Die Zahlungstatistik mit der angegebenen ID
     */
    public ZahlungStatistik getZahlungStatistikById(StatisticsDTO statisticDTO) {
        return zahlungsStatistikRepositoryService.findById(statisticDTO.getId());
    }

    /**
     * Löscht eine Zahlungstatistik basierend auf der ID.
     *
     */
    public void deleteZahlungStatistik(StatisticsDTO statisticDTO) {
        if (zahlungsStatistikRepositoryService.findById(statisticDTO.getId()) != null) {
            zahlungsStatistikRepositoryService.deleteById(statisticDTO.getId());
        }
    }

    public ZahlungStatistik createZahlungStatistik(StatisticsDTO statisticDTO) {

        FundEntity fund = fundRepositoryService.findFundById(statisticDTO.getFundId());

        // Start- und Enddatum parsen
        LocalDate start = LocalDate.parse(statisticDTO.getStartDate());
        LocalDate end = LocalDate.parse(statisticDTO.getEndDate());

        // Zahlungen im angegebenen Zeitraum filtern
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

        // Zahlungstatistik erstellen
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
            zahlungStatistik.setCurrency("EUR"); // Standardwert, wenn keine Zahlungen vorhanden sind
        }

        return zahlungsStatistikRepositoryService.saveStatistic(zahlungStatistik);
    }

    /**
     * Ruft alle Fonds ab, die dem Benutzer zugeordnet sind, basierend auf dem Access Token.
     *
     * @param accessToken Das Access Token des Benutzers
     * @return Eine Liste der Fonds, die mit dem Benutzer verknüpft sind
     */
    public List<FundEntity> getAllFunds(String accessToken) {
        // Überprüfe, ob der Benutzer anhand des Tokens existiert
        UserEntity user = userRepositoryService.findByToken(accessToken);
        if (user == null) {
            throw new IllegalArgumentException("Ungültiges Access Token oder Benutzer nicht gefunden.");
        }

        // Liste der Fonds abrufen, die dem Benutzer zugeordnet sind
        List<FundEntity> funds = fundRepositoryService.findALl();
        funds = funds.stream()
                .filter(f -> f.getUsers().contains(user))  // Filtere die Fonds, die mit diesem Benutzer verknüpft sind
                .toList();

        return funds;
    }

    /**
     * Erstellt oder aktualisiert eine Zahlungstatistik, basierend auf den übermittelten Daten.
     *
     * @return Die erstellte oder aktualisierte Zahlungstatistik
     */
    public ZahlungStatistik createOrUpdateZahlungStatistik(StatisticsDTO statisticDTO) {

        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;
        LocalDate startDate = LocalDate.parse(statisticDTO.getStartDate(), formatter);
        LocalDate endDate = LocalDate.parse(statisticDTO.getEndDate(), formatter);

        // Überprüfen, ob eine bestehende Statistik vorhanden ist
        ZahlungStatistik existing = zahlungsStatistikRepositoryService.findById(statisticDTO.getId());

        if (existing != null) {
            FundEntity fund = fundRepositoryService.findFundById(statisticDTO.getFundId());
            // Aktualisieren der bestehenden Statistik
            List<Payment> paymentsInPeriod = fund.getPayments().stream()
                    .filter(payment -> {
                        LocalDate paymentDate = payment.getDate().toLocalDate();
                        return !paymentDate.isBefore(startDate) && !paymentDate.isAfter(endDate);
                    })
                    .toList();

            // Berechnungen durchführen
            double totalAmount = paymentsInPeriod.stream().mapToDouble(Payment::getAmount).sum();
            long paymentCount = paymentsInPeriod.size();
            double averageAmount = paymentCount > 0 ? totalAmount / paymentCount : 0.0;

            existing.setFund(fundRepositoryService.findFundById(statisticDTO.getFundId()));
            existing.setStartDate(startDate);
            existing.setEndDate(endDate);
            existing.setTotalAmount(totalAmount);
            existing.setAverageAmount(averageAmount);
            existing.setPaymentCount(paymentCount);
            return zahlungsStatistikRepositoryService.saveStatistic(existing);
        } else {
            return createZahlungStatistik(statisticDTO);
        }
    }

    /**
     * Extrahiert das Access Token aus dem Authorization-Header.
     *
     * @param authorizationHeader Der Authorization-Header im Format "Bearer <token>"
     * @return Das Access Token
     * @throws IllegalArgumentException Wenn der Header nicht im erwarteten Format ist
     */
    public String extractAccessToken(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7); // Entfernt "Bearer " und gibt den Token zurück
        } else {
            throw new IllegalArgumentException("Der Authorization-Header muss im Format 'Bearer <token>' sein.");
        }
    }
}