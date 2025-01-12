package com.springboot.vitalorganize.service;

import com.springboot.vitalorganize.entity.*;
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
    public List<ZahlungStatistik> getAllZahlungStatistiken(Long fundId) {
        return zahlungsStatistikRepositoryService.findAllByFundId(fundRepositoryService.findFundById(fundId));
    }

    /**
     * Ruft eine Zahlungstatistik basierend auf der ID ab.
     *
     * @param id Die ID der Zahlungstatistik
     * @return Die Zahlungstatistik mit der angegebenen ID
     */
    public ZahlungStatistik getZahlungStatistikById(Long id) {
        return zahlungsStatistikRepositoryService.findById(id);
    }

    /**
     * Löscht eine Zahlungstatistik basierend auf der ID.
     *
     * @param id Die ID der Zahlungstatistik, die gelöscht werden soll
     */
    public void deleteZahlungStatistik(Long id) {
        if (zahlungsStatistikRepositoryService.findById(id) != null) {
            zahlungsStatistikRepositoryService.deleteById(id);
        }
    }

    /**
     * Erstellt eine Zahlungstatistik mit minimalen Eingabedaten.
     *
     * @param fundId Die ID des Fonds, für den die Statistik erstellt werden soll
     * @param startDate Das Startdatum der Zahlungsperiode im Format "yyyy-MM-dd"
     * @param endDate Das Enddatum der Zahlungsperiode im Format "yyyy-MM-dd"
     * @return Die erstellte Zahlungstatistik
     */
    public ZahlungStatistik createZahlungStatistikWithMinimalInput(Long fundId, String startDate, String endDate) {
        // Fund abrufen
        FundEntity fund = fundRepositoryService.findFundById(fundId);

        // Start- und Enddatum parsen
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);

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
     * @param id Die ID der Zahlungstatistik (null für eine neue Statistik)
     * @return Die erstellte oder aktualisierte Zahlungstatistik
     */
    public ZahlungStatistik createOrUpdateZahlungStatistik(Long id, Long fundId, String start, String end) {

        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;
        LocalDate startDate = LocalDate.parse(start, formatter);
        LocalDate endDate = LocalDate.parse(end, formatter);

        // Überprüfen, ob eine bestehende Statistik vorhanden ist
        ZahlungStatistik existing = zahlungsStatistikRepositoryService.findById(id);

        if (existing != null) {
            FundEntity fund = fundRepositoryService.findFundById(fundId);
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

            existing.setFund(fundRepositoryService.findFundById(fundId));
            existing.setStartDate(startDate);
            existing.setEndDate(endDate);
            existing.setTotalAmount(totalAmount);
            existing.setAverageAmount(averageAmount);
            existing.setPaymentCount(paymentCount);
            return zahlungsStatistikRepositoryService.saveStatistic(existing);
        } else {
            return createZahlungStatistikWithMinimalInput(fundId, start, end);
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