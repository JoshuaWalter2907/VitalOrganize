package com.springboot.vitalorganize.controller;

import com.springboot.vitalorganize.service.ReportService;
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
    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/send-report")
    public String sendReport(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam("userId") int userId,
            @RequestParam("recipientEmail") String recipientEmail) {
        try {
            reportService.sendReport(userId, startDate, endDate, recipientEmail);
            return "Report sent successfully!";
        } catch (Exception e) {
            e.printStackTrace();
            return "Failed to send report: " + e.getMessage();
        }
    }
}
