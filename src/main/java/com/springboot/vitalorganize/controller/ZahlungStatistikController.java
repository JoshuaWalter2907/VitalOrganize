package com.springboot.vitalorganize.controller;

import com.springboot.vitalorganize.dto.ZahlungStatistikRequest;
import com.springboot.vitalorganize.model.FundEntity;
import com.springboot.vitalorganize.model.ZahlungStatistik;
import com.springboot.vitalorganize.repository.ZahlungStatistikRepository;
import com.springboot.vitalorganize.service.ZahlungStatistikService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@AllArgsConstructor
public class ZahlungStatistikController {

    private ZahlungStatistikService zahlungStatistikService;


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
        return ResponseEntity.ok(zahlungStatistikService.getAllFunds(zahlungStatistikService.extractAccessToken(authorizationHeader)));
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
            @RequestBody ZahlungStatistikRequest request) {

        ZahlungStatistik result = zahlungStatistikService.createOrUpdateZahlungStatistik(id, request);
        return ResponseEntity.ok(result);
    }

    // Delete
    @DeleteMapping("/statistics/{id}")
    public ResponseEntity<Void> deleteZahlungStatistik(@PathVariable Long id) {
        zahlungStatistikService.deleteZahlungStatistik(id);
        return ResponseEntity.noContent().build();
    }

    @RequestMapping("/**")  // Fängt alle Anfragen ab, die nicht zu den oben definierten passen
    public ResponseEntity<String> handleInvalidRequests() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Invalid endpoint or bad request");
    }

}
