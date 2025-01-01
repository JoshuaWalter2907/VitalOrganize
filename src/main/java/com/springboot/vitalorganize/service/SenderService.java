package com.springboot.vitalorganize.service;

import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import com.springboot.vitalorganize.model.PersonalInformation;
import com.springboot.vitalorganize.model.UserEntity;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.AllArgsConstructor;
import org.apache.tomcat.util.http.fileupload.ByteArrayOutputStream;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
@AllArgsConstructor
public class SenderService {

    private final JavaMailSender mailSender;


    @Async
    public void createPdf(UserEntity benutzer) {
        String email = benutzer.getEmail();
        if (benutzer.getProvider().equals("github"))
            email = benutzer.getSendtoEmail();

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            String password = generateRandomPassword();


            sendEmail(
                    email,
                    "Ihr PDF Passwort",
                    "Das Passwort für Ihr PDF lautet: " + password
            );


            Document document = new Document();
            PdfWriter writer = PdfWriter.getInstance(document, outputStream);

            writer.setEncryption(
                    password.getBytes(),
                    null, // Benutzer-Passwort (Owner Passwort kann leer sein)
                    PdfWriter.ALLOW_PRINTING,
                    PdfWriter.ENCRYPTION_AES_128
            );

            document.open();

            document.add(new Paragraph("Benutzerinformationen", new Font(Font.HELVETICA, 16, Font.BOLD)));
            document.add(new Paragraph("Email: " + benutzer.getEmail()));
            document.add(new Paragraph("Nutzername: " + (benutzer.getUsername() != null ? benutzer.getUsername() : "N/A")));
            document.add(new Paragraph("Geburtstag: " + (benutzer.getBirthday() != null ? benutzer.getBirthday().toString() : "N/A")));

            if (benutzer.getPersonalInformation() != null) {
                PersonalInformation personalInfo = benutzer.getPersonalInformation();
                document.add(new Paragraph("\nPersönliche Informationen", new Font(Font.HELVETICA, 14, Font.BOLD)));
                document.add(new Paragraph("Vorname: " + (personalInfo.getFirstName() != null ? personalInfo.getFirstName() : "N/A")));
                document.add(new Paragraph("Nachname: " + (personalInfo.getLastName() != null ? personalInfo.getLastName() : "N/A")));
                document.add(new Paragraph("Adresse: " + (personalInfo.getAddress() != null ? personalInfo.getAddress() : "N/A")));
                document.add(new Paragraph("Postleitzahl: " + (personalInfo.getPostalCode() != null ? personalInfo.getPostalCode() : "N/A")));
                document.add(new Paragraph("Stadt: " + (personalInfo.getCity() != null ? personalInfo.getCity() : "N/A")));
                document.add(new Paragraph("Region: " + (personalInfo.getRegion() != null ? personalInfo.getRegion() : "N/A")));
                document.add(new Paragraph("Land: " + (personalInfo.getCountry() != null ? personalInfo.getCountry() : "N/A")));
            } else {
                document.add(new Paragraph("\nPersönliche Informationen nicht verfügbar."));
            }

            document.close();
            sendEmailWithAttachment(
                    email,
                    "Ihre Benutzerinformationen",
                    "Anbei finden Sie Ihre Benutzerinformationen als PDF.",
                    "user_info.pdf",
                    new ByteArrayResource(outputStream.toByteArray())
            );
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public void sendEmailWithAttachment(String to, String subject, String body, String fileName, ByteArrayResource attachment) {
        MimeMessage message = mailSender.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body);
            helper.addAttachment(fileName, attachment);

            mailSender.send(message);
        } catch (MessagingException e) {
            System.out.println(e.getMessage());
        }
    }

    private String generateRandomPassword() {
        SecureRandom random = new SecureRandom();
        int length = 12;
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@#$%!";
        StringBuilder password = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            password.append(characters.charAt(random.nextInt(characters.length())));
        }
        return password.toString();
    }

    public void sendConfirmationEmail(
            String email, String amount, String currency, String method,
            String description, String type
    ) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("Zahlungsbestätigung - PayPal");
            message.setText(String.format(
                    "Sehr geehrte/r Kunde/in,\n\n" +
                            "wir haben Ihre PayPal-Zahlung erhalten. Hier sind die Details Ihrer Transaktion:\n\n" +
                            "-----------------------------------\n" +
                            "Zahlungsbetrag: %s %s\n" +
                            "Zahlungsmethode: %s\n" +
                            "Transaktionsbeschreibung: %s\n" +
                            "Transaktionstyp: %s\n" +
                            "-----------------------------------\n\n" +
                            "Mit freundlichen Grüßen,\n" +
                            "Ihr Team"
                    ,
                    amount, currency, method, description, type
            ));
            mailSender.send(message);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public void sendEmail(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);

            mailSender.send(message);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

}
