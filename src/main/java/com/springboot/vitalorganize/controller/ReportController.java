package com.springboot.vitalorganize.controller;

import com.springboot.vitalorganize.service.ReportService;
import com.springboot.vitalorganize.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
public class ReportController {

    private final ReportService reportService;

    @Autowired
    public UserService userService;


    @Autowired
    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    /**
     * Endpunkt zum Versenden eines Reports für den angegebenen Zeitraum.
     */
    @GetMapping("/send-report")
    public String sendReport(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        try {
            // Report für den aktuellen Benutzer im angegebenen Zeitraum versenden
            reportService.sendReport(userService.getCurrentUser().getId(),
                    startDate, endDate,
                    userService.getCurrentUser().getEmail());
            return "Report sent successfully!"; // Erfolgreiche Nachricht zurückgeben
        } catch (Exception e) {
            e.printStackTrace(); // Fehler ausgeben, falls etwas schief geht
            return "Failed to send report: " + e.getMessage(); // Fehlermeldung zurückgeben
        }
    }
}
