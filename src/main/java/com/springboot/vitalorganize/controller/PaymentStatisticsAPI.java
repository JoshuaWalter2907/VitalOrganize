package com.springboot.vitalorganize.controller;

import com.springboot.vitalorganize.entity.FundEntity;
import com.springboot.vitalorganize.entity.ZahlungStatistik;
import com.springboot.vitalorganize.model.StatisticsDTO;
import com.springboot.vitalorganize.service.ZahlungStatistikService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;



/**
 * Controller zur Verwaltung des REST Endpoints
 */
@RestController
@RequestMapping("/api")
@AllArgsConstructor
public class PaymentStatisticsAPI {

    private final ZahlungStatistikService zahlungStatistikService;

    /**
     * Endpoint der REST API um alle Statistiken zu erhalten
     * @param statisticDTO Alle Informationen, die per Parameter übergeben werden
     * @return Standardantwort einer REST API
     */
    @PostMapping
    public ResponseEntity<ZahlungStatistik> createPaymentStatistics(StatisticsDTO statisticDTO) {
        ZahlungStatistik created = zahlungStatistikService.createZahlungStatistik(statisticDTO);
        return ResponseEntity.ok(created);
    }

    /**
     * Gibt eine Liste aller Fundstatistiken zurück.
     *
     * @param authorizationHeader der Authorization-Header mit Zugriffstoken
     * @return eine Liste der Statistiken
     */
    @GetMapping("/all/fund")
    public ResponseEntity<List<FundEntity>> getAllFundStatistics(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) {
        return ResponseEntity.ok(zahlungStatistikService.getAllFunds(zahlungStatistikService.extractAccessToken(authorizationHeader)));
    }

    /**
     * Gibt eine Liste aller gespeicherten Zahlungsstatistiken zurück.
     *
     * @return eine Liste von Zahlungsstatistiken
     */
    @GetMapping("/all/statistics")
    public ResponseEntity<List<ZahlungStatistik>> getAllZahlungStatistiken(
            StatisticsDTO statisticDTO
    ) {
        List<ZahlungStatistik> list = zahlungStatistikService.getAllZahlungStatistiken(statisticDTO);
        return ResponseEntity.ok(list);
    }

    /**
     * Ruft eine spezifische Zahlungsstatistik anhand der ID ab.
     *
     * @return die Zahlungsstatistik mit der angegebenen ID
     */
    @GetMapping("/statistics/{id}")
    public ResponseEntity<ZahlungStatistik> getZahlungStatistikById(
            StatisticsDTO statisticDTO
    ) {
        ZahlungStatistik zahlungStatistik = zahlungStatistikService.getZahlungStatistikById(statisticDTO);
        return ResponseEntity.ok(zahlungStatistik);
    }

    /**
     * Erstellt oder aktualisiert eine Zahlungsstatistik.
     *
     * @return die erstellte oder aktualisierte Zahlungsstatistik
     */
    @PutMapping("/statistics/{id}")
    public ResponseEntity<ZahlungStatistik> createOrUpdateZahlungStatistik(
            StatisticsDTO statisticDTO
    ) {
        ZahlungStatistik result = zahlungStatistikService.createOrUpdateZahlungStatistik(statisticDTO);
        return ResponseEntity.ok(result);
    }


    /**
     * Löscht eine Zahlungsstatistik anhand ihrer ID.
     *
     * @return eine Antwort ohne Inhalt, um den Erfolg anzuzeigen
     */
    @DeleteMapping("/statistics/{id}")
    public ResponseEntity<Void> deleteZahlungStatistik(
            StatisticsDTO statisticDTO
    ) {
        zahlungStatistikService.deleteZahlungStatistik(statisticDTO);
        return ResponseEntity.noContent().build();
    }

    /**
     * Behandelt ungültige oder nicht definierte API-Anfragen.
     *
     * @return eine Fehlermeldung mit dem Status BAD_REQUEST
     */
    @RequestMapping("/**")
    public ResponseEntity<String> handleInvalidRequests() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Invalid endpoint or bad request");
    }
}
