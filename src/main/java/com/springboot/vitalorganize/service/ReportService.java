package com.springboot.vitalorganize.service;

import com.springboot.vitalorganize.dao.MealDAO;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class ReportService {

    private final MealDAO mealDAO;
    private final JavaMailSender mailSender;
    private final String senderEmail;

    @Autowired
    public ReportService(MealDAO mealDAO, JavaMailSender mailSender, org.springframework.core.env.Environment env) {
        this.mealDAO = mealDAO;
        this.mailSender = mailSender;
        this.senderEmail = env.getProperty("spring.mail.username"); // E-Mail des Absenders aus der Konfiguration
    }

    /**
     * Sendet einen Bericht über die gekochten Rezepte und deren Kalorien per E-Mail.
     *
     * @param userId         Benutzer-ID, für die der Bericht erstellt werden soll
     * @param startDate      Startdatum des Berichtszeitraums
     * @param endDate        Enddatum des Berichtszeitraums
     * @param recipientEmail E-Mail-Adresse des Empfängers
     * @throws MessagingException wenn ein Fehler beim Senden der E-Mail auftritt
     */
    public void sendReport(Long userId, LocalDate startDate, LocalDate endDate, String recipientEmail) throws MessagingException {
        // Berichtsdaten aus der Datenbank durch den MealDAO abrufen
        List<Map<String, Object>> reportData = mealDAO.findMealReportForUser(userId, startDate, endDate);

        // Bericht als Text generieren
        String reportContent = generateReport(reportData);

        // E-Mail mit dem generierten Bericht senden
        sendEmail(recipientEmail, "Meal Report", reportContent);
    }

    /**
     * Generiert den Textinhalt für den Bericht basierend auf den abgerufenen Daten.
     *
     * @param reportData Liste der Berichtsdaten aus der Datenbank
     * @return Bericht als String, der die Rezepte und Kalorien auflistet
     */
    private String generateReport(List<Map<String, Object>> reportData) {
        StringBuilder reportBuilder = new StringBuilder();
        reportBuilder.append("Meal Report\n");
        reportBuilder.append("====================\n");
        reportBuilder.append("Date       | Recipe Name        | Calories\n");
        reportBuilder.append("------------------------------------------\n");

        int totalCalories = 0;

        // Durchlaufen der Berichtsdaten und Formatieren der Ausgabe
        for (Map<String, Object> row : reportData) {
            String date = row.get("meal_date").toString();
            String recipeName = row.get("recipe_name").toString();
            int calories = Integer.parseInt(row.get("calories").toString());

            totalCalories += calories;
            reportBuilder.append(String.format("%-10s | %-18s | %d\n", date, recipeName, calories));
        }

        // Gesamtkalorien zum Bericht hinzufügen
        reportBuilder.append("\nTotal Calories: ").append(totalCalories);
        return reportBuilder.toString();
    }

    /**
     * Sendet eine E-Mail mit dem angegebenen Inhalt.
     *
     * @param recipient Empfänger der E-Mail
     * @param subject   Betreff der E-Mail
     * @param content   Inhalt der E-Mail
     * @throws MessagingException wenn ein Fehler beim Senden der E-Mail auftritt
     */
    private void sendEmail(String recipient, String subject, String content) throws MessagingException {
        // Erstellen einer neuen E-Mail-Nachricht
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        // E-Mail-Einstellungen: Absender, Empfänger, Betreff und Inhalt
        helper.setFrom(senderEmail);
        helper.setTo(recipient);
        helper.setSubject(subject);
        helper.setText(content);

        // Senden der E-Mail
        mailSender.send(message);
    }
}
