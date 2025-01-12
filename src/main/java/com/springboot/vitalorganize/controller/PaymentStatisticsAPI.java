package com.springboot.vitalorganize.controller;

import com.springboot.vitalorganize.entity.FundEntity;
import com.springboot.vitalorganize.entity.ZahlungStatistik;
import com.springboot.vitalorganize.service.ZahlungStatistikService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;



/**
 * Controller zur Verwaltung von Zahlungsstatistiken und verwandten Aktionen wie dem Erstellen, Abrufen,
 * Aktualisieren und Löschen von Zahlungsstatistiken.
 * Bietet Endpunkte für die Interaktion mit den Zahlungsstatistiken sowie die Abfrage von Fondsstatistiken.
 */
@RestController
@RequestMapping("/api")
@AllArgsConstructor
public class PaymentStatisticsAPI {

    private final ZahlungStatistikService zahlungStatistikService;

    /**
     * Erstellt eine neue Zahlungsstatistik mit minimalem Eingabeaufwand.
     *
     * @param fundId    die ID des zugehörigen Fonds
     * @param startDate das Startdatum der Statistik
     * @param endDate   das Enddatum der Statistik
     * @return die erstellte Zahlungsstatistik
     */
    @PostMapping
    public ResponseEntity<ZahlungStatistik> createZahlungStatistik(@RequestParam Long fundId,
                                                                   @RequestParam String startDate,
                                                                   @RequestParam String endDate) {
        ZahlungStatistik created = zahlungStatistikService.createZahlungStatistikWithMinimalInput(fundId, startDate, endDate);
        return ResponseEntity.ok(created);
    }

    /**
     * Platzhalter-Endpunkt, um auf allgemeine API-Anfragen zu reagieren.
     *
     * @return eine Textantwort, dass diese Anfrage nicht relevant ist
     */
    @GetMapping
    public ResponseEntity<String> getAllZahlungStatistik() {
        return ResponseEntity.ok("not what you are looking for");
    }

    /**
     * Gibt eine Liste aller Fondsstatistiken zurück.
     *
     * @param authorizationHeader der Authorization-Header mit Zugriffstoken
     * @return eine Liste der Fondsstatistiken
     */
    @GetMapping("/all/fund")
    public ResponseEntity<List<FundEntity>> getAllFundStatistik(
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
            @RequestParam Long id
    ) {
        List<ZahlungStatistik> list = zahlungStatistikService.getAllZahlungStatistiken(id);
        return ResponseEntity.ok(list);
    }

    /**
     * Ruft eine spezifische Zahlungsstatistik anhand der ID ab.
     *
     * @param id die ID der gesuchten Zahlungsstatistik
     * @return die Zahlungsstatistik mit der angegebenen ID
     */
    @GetMapping("/statistics/{id}")
    public ResponseEntity<ZahlungStatistik> getZahlungStatistikById(@PathVariable Long id) {
        ZahlungStatistik zahlungStatistik = zahlungStatistikService.getZahlungStatistikById(id);
        return ResponseEntity.ok(zahlungStatistik);
    }

    /**
     * Erstellt oder aktualisiert eine Zahlungsstatistik.
     *
     * @param id      die ID der zu aktualisierenden oder zu erstellenden Zahlungsstatistik
     * @return die erstellte oder aktualisierte Zahlungsstatistik
     */
    @PutMapping("/statistics/{id}")
    public ResponseEntity<ZahlungStatistik> createOrUpdateZahlungStatistik(
            @PathVariable Long id,
            @RequestParam(required = false) Long fundId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
            ) {
        ZahlungStatistik result = zahlungStatistikService.createOrUpdateZahlungStatistik(id, fundId, startDate, endDate);
        return ResponseEntity.ok(result);
    }


    /**
     * Löscht eine Zahlungsstatistik anhand ihrer ID.
     *
     * @param id die ID der zu löschenden Zahlungsstatistik
     * @return eine Antwort ohne Inhalt, um den Erfolg anzuzeigen
     */
    @DeleteMapping("/statistics/{id}")
    public ResponseEntity<Void> deleteZahlungStatistik(@PathVariable Long id) {
        zahlungStatistikService.deleteZahlungStatistik(id);
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
