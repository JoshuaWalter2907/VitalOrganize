package com.springboot.vitalorganize.controller;

import com.springboot.vitalorganize.model.FundEntity;
import com.springboot.vitalorganize.model.ZahlungStatistik;
import com.springboot.vitalorganize.repository.ZahlungStatistikRepository;
import com.springboot.vitalorganize.service.ZahlungStatistikService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ZahlungStatistikController {

    @Autowired
    private ZahlungStatistikService zahlungStatistikService;
    @Autowired
    private ZahlungStatistikRepository zahlungStatistikRepository;


    // Create
    @PostMapping
    public ResponseEntity<ZahlungStatistik> createZahlungStatistik(@RequestParam Long fundId,
                                                                   @RequestParam String startDate,
                                                                   @RequestParam String endDate) {
        ZahlungStatistik created = zahlungStatistikService.createZahlungStatistikWithMinimalInput(fundId, startDate, endDate);
        return ResponseEntity.ok(created);
    }

    @GetMapping
    public ResponseEntity<String > getAllZahlungStatistik() {
        return ResponseEntity.ok("not what you are looking for");
    }


    @GetMapping("/all/fund")
    public ResponseEntity<List<FundEntity>> getAllFundStatistik(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader
    ) {
        return ResponseEntity.ok(zahlungStatistikService.getAllFunds(extractAccessToken(authorizationHeader)));
    }
    // Read all
    @GetMapping("/all/statistics")
    public ResponseEntity<List<ZahlungStatistik>> getAllZahlungStatistiken() {
        List<ZahlungStatistik> list = zahlungStatistikService.getAllZahlungStatistiken();
        return ResponseEntity.ok(list);
    }

    // Read by ID
    @GetMapping("/statistics/{id}")
    public ResponseEntity<ZahlungStatistik> getZahlungStatistikById(@PathVariable Long id) {
        ZahlungStatistik zahlungStatistik = zahlungStatistikService.getZahlungStatistikById(id);
        return ResponseEntity.ok(zahlungStatistik);
    }

    // Update
    @PutMapping("/statistics/{id}")
    public ResponseEntity<ZahlungStatistik> createOrUpdateZahlungStatistik(
            @PathVariable Long id,
            @RequestParam Long fundId,
            @RequestParam String startDate,
            @RequestParam String endDate) {

        // Prüfen, ob eine ZahlungStatistik für die gegebene ID existiert
        ZahlungStatistik existing = zahlungStatistikRepository.findById(id)
                .orElse(null); // Falls keine existiert, wird null zurückgegeben.

        if (existing != null) {
            // Falls eine existierende Statistik gefunden wird, können wir sie aktualisieren.
            existing = zahlungStatistikService.updateZahlungStatistik(id, fundId, startDate, endDate);
            return ResponseEntity.ok(existing);
        } else {
            // Wenn keine Statistik vorhanden ist, erstellen wir eine neue
            ZahlungStatistik created = zahlungStatistikService.createZahlungStatistikWithMinimalInput(fundId, startDate, endDate);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        }
    }

    // Delete
    @DeleteMapping("/statistics/{id}")
    public ResponseEntity<Void> deleteZahlungStatistik(@PathVariable Long id) {
        zahlungStatistikService.deleteZahlungStatistik(id);
        return ResponseEntity.noContent().build();
    }

    private String extractAccessToken(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7); // Entfernt "Bearer " und gibt den Token zurück
        } else {
            throw new IllegalArgumentException("Authorization header must be in the form 'Bearer <token>'");
        }
    }

    @RequestMapping("/**")  // Fängt alle Anfragen ab, die nicht zu den oben definierten passen
    public ResponseEntity<String> handleInvalidRequests() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Invalid endpoint or bad request");
    }

}
