package com.springboot.vitalorganize.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import com.paypal.api.payments.*;
import com.paypal.api.payments.Currency;
import com.paypal.base.rest.APIContext;
import com.paypal.base.rest.PayPalRESTException;
import com.springboot.vitalorganize.config.PayPalConfig;
import com.springboot.vitalorganize.model.*;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.AllArgsConstructor;
import org.apache.tomcat.util.http.fileupload.ByteArrayOutputStream;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Service
@AllArgsConstructor
public class PaypalService {

    public final APIContext apiContext;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;
    private final subscriptionRepository subscriptionRepository;

    private final PayPalConfig payPalConfig;

    private static final String PAYPAL_API_URL = "https://api-m.sandbox.paypal.com/v1/billing/subscriptions";
    private final FundRepository fundRepository;
    private final DataSourceTransactionManagerAutoConfiguration dataSourceTransactionManagerAutoConfiguration;


    public String handlePaymentCreation(
            double amount,
            String currency,
            String description,
            String cancelUrl,
            String successUrl
    ) throws PayPalRESTException {
        Payment payment = createPayment(amount, currency, "paypal", "sale", description, cancelUrl, successUrl);

        for (Links link : payment.getLinks()) {
            if (link.getRel().equals("approval_url")) {
                return link.getHref();
            }
        }

        throw new PayPalRESTException("Approval URL not found in the payment response.");
    }

    public Payment createPayment(
            double total,
            String currency,
            String method,
            String intent,
            String description,
            String cancelUrl,
            String successUrl
    ) throws PayPalRESTException {
        Amount amount = new Amount();
        amount.setCurrency(currency);
        amount.setTotal(String.format(Locale.forLanguageTag(currency), "%.2f", total));

        Transaction transaction = new Transaction();
        transaction.setDescription(description);
        transaction.setAmount(amount);

        List<Transaction> transactions = new ArrayList<>();
        transactions.add(transaction);

        Payer payer = new Payer();
        payer.setPaymentMethod(method);

        Payment payment = new Payment();
        payment.setIntent(intent);
        payment.setPayer(payer);
        payment.setTransactions(transactions);

        RedirectUrls redirectUrls = new RedirectUrls();
        redirectUrls.setCancelUrl(cancelUrl);
        redirectUrls.setReturnUrl(successUrl);

        payment.setRedirectUrls(redirectUrls);

        return payment.create(apiContext);
    }

    public Payment executePayment(
            String paymentId,
            String payerId
    ) throws PayPalRESTException {

        Payment payment = new Payment();
        payment.setId(paymentId);

        PaymentExecution paymentExecution = new PaymentExecution();
        paymentExecution.setPayerId(payerId);

        return payment.execute(apiContext, paymentExecution);
    }

    @Transactional
    public Zahlung addPayment(Zahlung payment, Long id) {
        // Letzte Buchung abrufen
        Optional<Zahlung> latestTransaction = paymentRepository.findLatestTransactionByFundId(id);
        System.out.println(latestTransaction);
        double lastBalance = latestTransaction.map(Zahlung::getBalance).orElse(0.0);

        // Neuen Kontostand berechnen
        if (payment.getType() == PaymentType.EINZAHLEN) {
            payment.setBalance(lastBalance + payment.getAmount());
        } else if (payment.getType() == PaymentType.AUSZAHLEN) {
            payment.setBalance(lastBalance - payment.getAmount());
        }

        // Neue Buchung speichern
        System.out.println(payment);
        return paymentRepository.save(payment);
    }


    public double getCurrentBalance() {
        return paymentRepository.findLatestTransaction()
                .map(Zahlung::getBalance)
                .orElse(0.0);
    }

    public String getAccessToken() {
        String url = "https://api-m.sandbox.paypal.com/v1/oauth2/token";
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth(payPalConfig.getClientId(), payPalConfig.getClientSecret());

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "client_credentials");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, request, Map.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            Map<String, String> body = response.getBody();

            String accessToken = body.get("access_token");

            System.out.println(accessToken);
            return body.get("access_token");
        } else {
            throw new RuntimeException("Failed to fetch access token: " + response.getStatusCode());
        }
    }

    public String executePayout(String recipientEmail, String amount, String currency) throws PayPalRESTException {

        // 1. Sender Batch Header (Metadaten)
        PayoutSenderBatchHeader senderBatchHeader = new PayoutSenderBatchHeader();
        senderBatchHeader.setEmailSubject("You have a payout!");  // Betreff der Auszahlung
        senderBatchHeader.setSenderBatchId(UUID.randomUUID().toString());  // Eindeutige Batch-ID für diese Auszahlung

        // 2. Payout Item (Empfänger der Auszahlung)
        PayoutItem payoutItem = new PayoutItem();
        payoutItem.setRecipientType("EMAIL");  // Auszahlung an E-Mail
        payoutItem.setReceiver(recipientEmail);  // E-Mail des Empfängers
        payoutItem.setAmount(new Currency(currency, amount));  // Betrag und Währung
        payoutItem.setNote("Thank you!");  // Optionale Nachricht
        payoutItem.setSenderItemId(UUID.randomUUID().toString());  // Eindeutige Item-ID für diese Auszahlung

        System.out.println(payoutItem);

        // 3. Liste der Payout Items erstellen
        List<PayoutItem> items = new ArrayList<>();
        items.add(payoutItem);

        // 4. Payout-Objekt erstellen
        Payout payout = new Payout();
        payout.setSenderBatchHeader(senderBatchHeader);
        payout.setItems(items);

        System.out.println(payout);

        // 5. APIContext (für Authentifizierung)
        String accessToken = getAccessToken();  // Hol dir das AccessToken für die API

        // 6. API Anfrage erstellen
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Die Auszahlung JSON anpassen
        String jsonPayload = "{" +
                "\"sender_batch_header\": {" +
                "\"email_subject\": \"You have a payout!\"," +
                "\"sender_batch_id\": \"" + senderBatchHeader.getSenderBatchId() + "\"" +
                "}," +
                "\"items\": [" +
                "{" +
                "\"recipient_type\": \"EMAIL\"," +
                "\"amount\": {" +
                "\"currency\": \"" + currency + "\"," +
                "\"value\": \"" + amount + "\"" +
                "}," +
                "\"receiver\": \"" + recipientEmail + "\"," +
                "\"note\": \"Thank you!\"," +
                "\"sender_item_id\": \"" + payoutItem.getSenderItemId() + "\"" +
                "}" +
                "]" +
                "}";

        HttpEntity<String> requestEntity = new HttpEntity<>(jsonPayload, headers);
        String url = "https://api.sandbox.paypal.com/v1/payments/payouts";
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);

        // Überprüfe, ob die Anfrage erfolgreich war
        if (response.getStatusCode() == HttpStatus.CREATED) {
            // Erfolgreiche Antwort erhalten, nun die JSON-Antwort verarbeiten
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                Map<String, Object> responseMap = objectMapper.readValue(response.getBody(), Map.class);

                // Extrahiere die Payout Batch ID und den Status
                Map<String, Object> batchHeader = (Map<String, Object>) responseMap.get("batch_header");
                String payoutBatchId = (String) batchHeader.get("payout_batch_id");
                String batchStatus = (String) batchHeader.get("batch_status");

                return "Payout erfolgreich! Batch ID: " + payoutBatchId;
            } catch (Exception e) {
                throw new PayPalRESTException("Fehler beim Parsen der PayPal-Antwort: " + e.getMessage());
            }
        } else {
            throw new PayPalRESTException("Fehler bei der Auszahlung. HTTP Status: " + response.getStatusCode());
        }
    }

    public String getEmailForUser(OAuth2User user, String provider) {
        if ("github".equals(provider)) {
            String username = user.getAttribute("login");
            return username + "@github.com"; // Dummy-E-Mail für GitHub
        }
        return user.getAttribute("email");
    }

    public String processPayment(
            String paymentId, String payerId, String type, String amount,
            String currency, String description, String receiverEmail,
            String email, String provider, Long id, Long fundId
    ) throws Exception {
        System.out.println("Hier war ich noch");
        // Benutzer aktualisieren
        UserEntity user = userRepository.findByEmailAndProvider(email, provider);
        if (user.getProvider().equals("github"))
            email = user.getSendtoEmail();

        FundEntity fund = fundRepository.findById(fundId).get();
        System.out.println(fund);

        // Zahlung initialisieren
        Zahlung zahlung = new Zahlung();
        zahlung.setDate(LocalDateTime.now());
        zahlung.setReason(description);
        zahlung.setAmount(Double.valueOf(amount));
        zahlung.setCurrency(currency);
        zahlung.setUser(user);
        zahlung.setFund(fund);

        System.out.println(zahlung);

        if ("EINZAHLEN".equalsIgnoreCase(type)) {
            zahlung.setType(PaymentType.EINZAHLEN);
            Payment payment = executePayment(paymentId, payerId);
            if ("approved".equals(payment.getState())) {
                addPayment(zahlung, id);
                sendConfirmationEmail(email, amount, currency, "paypal", description, type);
                return "/fund";
            }
        } else if ("AUSZAHLEN".equalsIgnoreCase(type)) {
            double balance = getCurrentBalance();
            if (Double.valueOf(amount) > balance) {
                return "/fund/payinto/error";
            } else {
                zahlung.setType(PaymentType.AUSZAHLEN);
                executePayout(receiverEmail, amount, currency);
                System.out.println("Hier war ich auch noch");
                addPayment(zahlung, id);
                sendConfirmationEmail(email, amount, currency, "paypal", description, type);
                return "/fund";
            }
        }
        throw new RuntimeException("Invalid payment type");
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

    @Async
    public void createPdf(UserEntity benutzer) {
        String email = benutzer.getEmail();
        if (benutzer.getProvider().equals("github"))
            email = benutzer.getSendtoEmail();

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            String password = generateRandomPassword();


            // 2. Passwort per E-Mail senden
            sendEmail(
                    email,
                    "Ihr PDF Passwort",
                    "Das Passwort für Ihr PDF lautet: " + password
            );


            // PDF erstellen
            Document document = new Document();
            PdfWriter writer = PdfWriter.getInstance(document, outputStream);

            writer.setEncryption(
                    password.getBytes(),
                    null, // Benutzer-Passwort (Owner Passwort kann leer sein)
                    PdfWriter.ALLOW_PRINTING,
                    PdfWriter.ENCRYPTION_AES_128
            );

            document.open();

            // Benutzerinformationen in PDF einfügen
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
            // PDF an Benutzer per E-Mail senden (als Anhang)
            sendEmailWithAttachment(
                    email,
                    "Ihre Benutzerinformationen",
                    "Anbei finden Sie Ihre Benutzerinformationen als PDF.",
                    "user_info.pdf",
                    new ByteArrayResource(outputStream.toByteArray())
            );
        } catch (Exception e) {
            // Fehlerprotokollierung
            System.out.println(e.getMessage());
            ;
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

    @Transactional
    public void updateUserSubscriptionStatus(UserEntity user, boolean isSubscribed, String approvalUrl) {
        // Subscription-Status aktualisieren
        user.setRole("MEMBER");

        // Optional: Subscription-Daten wie ID oder Plan speichern
        userRepository.save(user);  // Änderungen in der Datenbank speichern
    }

    public String createSubscription(String planId, UserEntity user) {
        System.out.println("ich war hier");
        try {
            // 1. OAuth-Token abrufen
            String accessToken = getAccessToken();

            // 2. Die Subscription-Daten mit der Plan-ID erstellen
            String subscriptionRequest = createSubscriptionRequest(planId, user);

            // 3. Sende die Anfrage an PayPal
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(subscriptionRequest, headers);

            ResponseEntity<String> response = restTemplate.exchange(PAYPAL_API_URL, HttpMethod.POST, entity, String.class);

            // 4. Wenn die Subscription erfolgreich erstellt wurde, die Approval URL extrahieren
            String approvalUrl = extractApprovalUrl(response.getBody());


            return approvalUrl;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String createSubscriptionRequest(String planId, UserEntity user) {
        String email = user.getEmail();
        if (user.getProvider().equals("github"))
            email = user.getSendtoEmail();

        String returnUrl = "http://localhost:8080/subscription-success?userId=" + user.getId();
        String cancelUrl = "http://localhost:8080/subscription-cancel?userId=" + user.getId();

        return "{"
                + "\"plan_id\": \"" + planId + "\","
                + "\"subscriber\": {"
                + "    \"email_address\": \"" + email + "\""
                + "},"
                + "\"application_context\": {"
                + "    \"return_url\": \"" + returnUrl + "\","
                + "    \"cancel_url\": \"" + cancelUrl + "\""
                + "}"
                + "}";
    }

    private String extractApprovalUrl(String responseBody) {
        try {
            // Erstelle ein ObjectMapper-Objekt
            ObjectMapper objectMapper = new ObjectMapper();

            // Parsen des JSON-Response-Body
            JsonNode rootNode = objectMapper.readTree(responseBody);

            // Durch die "links"-Array iterieren und nach dem "approve"-Link suchen
            JsonNode linksNode = rootNode.get("links");

            if (linksNode != null) {
                for (JsonNode link : linksNode) {
                    String rel = link.get("rel").asText();
                    if ("approve".equalsIgnoreCase(rel)) {
                        return link.get("href").asText(); // Rückgabe der "href"-URL
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;  // Falls keine Approval-URL gefunden wurde
    }

    public String getPayerIdFromSubscription(String subscriptionId) {
        try {
            // 1. OAuth-Token abrufen
            String accessToken = getAccessToken();  // Hole den Access Token

            // 2. Anfrage-URL mit der Subscription-ID
            String subscriptionUrl = "https://api-m.sandbox.paypal.com/v1/billing/subscriptions/" + subscriptionId;

            // 3. HttpURLConnection für die Anfrage einrichten
            HttpURLConnection httpConn = (HttpURLConnection) new URL(subscriptionUrl).openConnection();
            httpConn.setRequestMethod("GET");
            httpConn.setRequestProperty("Authorization", "Bearer " + accessToken);
            httpConn.setRequestProperty("Content-Type", "application/json");

            // 4. Antwort von PayPal verarbeiten
            InputStream responseStream = httpConn.getResponseCode() / 100 == 2
                    ? httpConn.getInputStream()
                    : httpConn.getErrorStream();

            Scanner s = new Scanner(responseStream).useDelimiter("\\A");
            String response = s.hasNext() ? s.next() : "";

            System.out.println(response);

            // 5. PayerID aus der Antwort extrahieren


            JsonObject jsonObject = JsonParser.parseString(response).getAsJsonObject();
            String payerId = jsonObject.getAsJsonObject("subscriber").get("payer_id").getAsString();

            if (payerId != null) {
                return payerId;  // Rückgabe der PayerID
            } else {
                return "PayerID konnte nicht abgerufen werden.";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Fehler bei der Abfrage der PayerID";
        }
    }

    public String confirmSubscription(String subscriptionId, String payerId) {
        String accessToken = getAccessToken();  // OAuth Token holen

        // 1. Hole die Subscription-Daten
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        // URL, um den aktuellen Status der Subscription zu holen
        String getSubscriptionUrl = "https://api-m.sandbox.paypal.com/v1/billing/subscriptions/" + subscriptionId;

        // Anfrage an PayPal senden
        ResponseEntity<String> response = restTemplate.exchange(getSubscriptionUrl, HttpMethod.GET, new HttpEntity<>(headers), String.class);

        // Status der Subscription prüfen
        String subscriptionStatus = parseSubscriptionStatus(response.getBody()); // Hier musst du den Status aus der Antwort extrahieren

        // 2. Wenn der Status nicht "suspended" ist, suspendiere die Subscription
        if (!"SUSPENDED".equalsIgnoreCase(subscriptionStatus)) {
            String suspendUrl = "https://api-m.sandbox.paypal.com/v1/billing/subscriptions/" + subscriptionId + "/suspend";
            response = restTemplate.exchange(suspendUrl, HttpMethod.POST, new HttpEntity<>(headers), String.class);

            // Überprüfe, ob das Suspendieren erfolgreich war
            if (!response.getStatusCode().is2xxSuccessful()) {
                return "Error suspending subscription.";
            }
        }

        // 3. Aktivieren der Subscription
        String activateUrl = "https://api-m.sandbox.paypal.com/v1/billing/subscriptions/" + subscriptionId + "/activate";
        String body = "{ \"payer_id\": \"" + payerId + "\" }"; // Falls PayerId erforderlich ist
        HttpEntity<String> entity = new HttpEntity<>(body, headers);
        response = restTemplate.exchange(activateUrl, HttpMethod.POST, entity, String.class);

        // Überprüfen, ob die Aktivierung erfolgreich war
        if (response.getStatusCode().is2xxSuccessful()) {
            return "approved";
        } else {
            return "failed";
        }
    }

    private String parseSubscriptionStatus(String responseBody) {
        try {
            // Jackson ObjectMapper zum Parsen des JSON-Antwort-Strings
            ObjectMapper objectMapper = new ObjectMapper();

            // Die JSON-Antwort in einen JsonNode (Baumstruktur) umwandeln
            JsonNode rootNode = objectMapper.readTree(responseBody);

            // Den Status der Subscription extrahieren
            String status = rootNode.path("status").asText();

            // Rückgabe des Status
            return status;
        } catch (Exception e) {
            // Fehlerbehandlung: Wenn etwas schief geht, eine Fehlermeldung zurückgeben
            e.printStackTrace();
            return "Error parsing subscription status";
        }
    }

    public boolean cancelSubscription(UserEntity user, String subscriptionId) {

        if(user.getLatestSubscription().getStatus().equals("CANCELLED")) {
            return true;
        }

        if (user.getLatestSubscription() != null) {
            SubscriptionEntity subscription = user.getLatestSubscription();

            // 1. Subscription bei PayPal kündigen
            boolean isPaypalCancelled = cancelPaypalSubscription(subscription.getSubscriptionId());

            if (!isPaypalCancelled) {
                return false; // PayPal-Abbruch fehlgeschlagen
            }

            // 2. Lokalen Status auf "CANCELLED" setzen
            subscription.setStatus("CANCELLED");
            subscriptionRepository.save(subscription);
            return true;
        }

        return false; // Subscription wurde nicht gefunden
    }

    private boolean cancelPaypalSubscription(String paypalSubscriptionId) {
        try {
            // Abrufen des OAuth2-Tokens
            String accessToken = getAccessToken();

            // Erstellen des URL-Endpunkts für die Kündigung
            String url = PAYPAL_API_URL + "/" + paypalSubscriptionId + "/cancel";

            // Request erstellen
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Kündigungsgrund (optional)
            String body = "{ \"reason\": \"User requested cancellation.\" }";

            HttpEntity<String> entity = new HttpEntity<>(body, headers);

            // HTTP DELETE Request an PayPal senden
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            return response.getStatusCode() == HttpStatus.NO_CONTENT; // NO_CONTENT (204) bedeutet Erfolg

        } catch (Exception e) {
            e.printStackTrace();
            return false; // Fehler bei der Kündigung
        }
    }

    public boolean pauseSubscription(UserEntity user, String subscriptionId) {

        if(user.getLatestSubscription().getStatus().equals("CANCELLED")) {
            return true;
        }

        if (user.getLatestSubscription() != null) {
            SubscriptionEntity subscription = user.getLatestSubscription();

            // 1. Subscription bei PayPal kündigen
            boolean isPaypalCancelled = pausePayPalSubscription(subscription.getSubscriptionId());

            if (!isPaypalCancelled) {
                return false; // PayPal-Abbruch fehlgeschlagen
            }

            // 2. Lokalen Status auf "CANCELLED" setzen
            subscription.setStatus("SUSPENDED");
            subscriptionRepository.save(subscription);
            return true;
        }

        return false; // Subscription wurde nicht gefunden
    }

    public boolean pausePayPalSubscription(String subscriptionId) {
        try {
            String accessToken = getAccessToken();

            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + accessToken);

            String requestBody = """
                    {
                        "reason": "User requested to pause the subscription"
                    }
                    """;

            HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);

            // Anfrage an PayPal senden, um die Subscription zu pausieren
            ResponseEntity<String> response = restTemplate.exchange(
                    PAYPAL_API_URL + "/" + subscriptionId + "/suspend",
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false; // Fehler bei der Kündigung
        }


    }

    public boolean resumeSubscription(UserEntity user, String subscriptionId) {

        if(user.getLatestSubscription().getStatus().equals("ACTIVE")) {
            return true;
        }

        if (user.getLatestSubscription() != null) {
            SubscriptionEntity subscription = user.getLatestSubscription();

            // 1. Subscription bei PayPal wieder aufnehmen
            boolean isPaypalResumed = resumePayPalSubscription(subscriptionId);

            if (!isPaypalResumed) {
                return false; // Wiederaufnahme bei PayPal fehlgeschlagen
            }

            // 2. Lokalen Status auf "ACTIVE" setzen
            subscription.setStatus("ACTIVE");
            subscriptionRepository.save(subscription);
            return true;
        }

        return false; // Keine gültige Subscription gefunden
    }

    public boolean resumePayPalSubscription(String subscriptionId) {
        try {
            String accessToken = getAccessToken();

            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + accessToken);

            String requestBody = """
                {
                    "reason": "User requested to resume the subscription"
                }
                """;

            HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);

            // Anfrage an PayPal senden, um die Subscription wieder aufzunehmen
            ResponseEntity<String> response = restTemplate.exchange(
                    PAYPAL_API_URL + "/" + subscriptionId + "/activate",
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
            return false; // Fehler bei der Wiederaufnahme
        }
    }

    public List<TransactionSubscription> getTransactionsForSubscription(String subscriptionId) {

        try {
            String accessToken = getAccessToken(); // Zugriffstoken holen

            String startTime = "2023-12-01T00:00:00Z"; // Anfang des Zeitraums
            String endTime = "2025-12-27T23:59:59Z";   // Ende des Zeitraums

            // Erstellen des Endpunkt-URLs
            String url = UriComponentsBuilder.fromHttpUrl(PAYPAL_API_URL + "/" + subscriptionId + "/transactions")
                    .queryParam("start_time", startTime)
                    .queryParam("end_time", endTime)
                    .toUriString();


            // RestTemplate für HTTP-Anfragen
            RestTemplate restTemplate = new RestTemplate();

            // Header für die Authentifizierung mit OAuth2 AccessToken
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Anfrage-Entity mit den Headern
            HttpEntity<String> entity = new HttpEntity<>(headers);

            // GET-Anfrage senden
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                // Wenn erfolgreich, verarbeite die Antwort hier (in diesem Fall JSON als String)
                String responseBody = response.getBody();
                System.out.println(responseBody);

                // Um die Antwort als List von Transaction-Objekten zu parsen, könntest du eine JSON-Bibliothek wie Jackson oder Gson verwenden
                List<TransactionSubscription> transactions = parseTransactions(responseBody);


                return transactions;
            } else {
                // Fehlerbehandlung bei unerfolgreicher Antwort
                System.out.println("Fehler bei der Transaktionsabfrage: " + response.getStatusCode());
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null; // Fehlerbehandlung
        }
    }

    private List<TransactionSubscription> parseTransactions(String jsonResponse) {
        // Erstelle den ObjectMapper und registriere das JavaTimeModule für die Unterstützung von java.time
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule()); // Registriert das Modul für Java 8 Zeittypen

        try {
            // Lies die gesamte JSON-Struktur
            JsonNode jsonNode = objectMapper.readTree(jsonResponse);
            // Extrahiere den "transactions"-Knoten
            JsonNode transactionsNode = jsonNode.path("transactions");

            // Verwende Jacksons Typreferenz für die Liste von TransactionSubscription-Objekten
            return objectMapper.readerForListOf(TransactionSubscription.class)
                    .readValue(transactionsNode);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>(); // Rückgabe einer leeren Liste im Fehlerfall
        }
    }

    public List<TransactionSubscription> filterTransactions(
            List<TransactionSubscription> transactions,
            String username, // Für Payer Name
            LocalDate datefrom, // Startdatum
            LocalDate dateto,   // Enddatum
            Long amount) {     // Transaktions-ID


        List<TransactionSubscription> filteredTransactions = transactions.stream()
                .peek(transaction -> System.out.println("Vor Filterung: " + transaction))
                .filter(transaction -> {
                    boolean condition = username == null || username.isEmpty() ||
                            (transaction.getPayerEmail() != null &&
                                    transaction.getPayerEmail().toLowerCase().contains(username.toLowerCase()));
                    System.out.println("Username-Filter (" + transaction.getPayerEmail() + "): " + condition);
                    return condition;
                })
                .filter(transaction -> {
                    boolean condition = datefrom == null ||
                            (transaction.getTime() != null && !transaction.getTime().toLocalDate().isBefore(datefrom));
                    System.out.println("DateFrom-Filter (" + transaction.getTime() + "): " + condition);
                    return condition;
                })
                .filter(transaction -> {
                    boolean condition = dateto == null ||
                            (transaction.getTime() != null && !transaction.getTime().toLocalDate().isAfter(dateto));
                    System.out.println("DateTo-Filter (" + transaction.getTime() + "): " + condition);
                    return condition;
                })
                .filter(transaction -> {
                    boolean condition = amount == null ||
                            (transaction.getAmountWithBreakdown() != null &&
                                    transaction.getAmountWithBreakdown().getGrossAmount() != null &&
                                    Double.parseDouble(transaction.getAmountWithBreakdown().getGrossAmount().getValue()) >= amount);
                    System.out.println("Amount-Filter (" + transaction.getAmountWithBreakdown() + "): " + condition);
                    return condition;
                })
                .peek(transaction -> System.out.println("Nach allen Filtern: " + transaction))
                .collect(Collectors.toList());

        System.out.println("Gefilterte Transaktionen: " + filteredTransactions);
        return filteredTransactions;
    }


    public List<Zahlung> filterPayments(List<Zahlung> payments,
                                        String username,
                                        String reason,
                                        LocalDate datefrom,
                                        LocalDate dateto,
                                        Long amount
    ) {
        List<Zahlung> filteredPayments = payments.stream()
                        .filter(payment -> {
                            String usernameToCheck = (payment.getUser() != null) ? payment.getUser().getUsername() : "Deleted User";
                            boolean condition = username == null || username.isEmpty() ||
                                    usernameToCheck.toLowerCase().contains(username.toLowerCase());
                            System.out.println("Username-Filter (" + usernameToCheck + "): " + condition);
                            return condition;
                        })

                        .filter(payment -> {
                            boolean condition = reason == null || reason.isEmpty() ||
                                    (payment.getReason() != null &&
                                            payment.getReason().toLowerCase().contains(reason.toLowerCase()));
                            System.out.println("Reason-Filter (" + payment.getReason() + "): " + condition);
                            return condition;
                        })
                        .filter(payment -> {
                            boolean condition = datefrom == null ||
                                    (payment.getDate() != null && !payment.getDate().toLocalDate().isBefore(datefrom));
                            System.out.println("DateFrom-Filter (" + payment.getDate() + "): " + condition);
                            return condition;
                        })
                        .filter(payment -> {
                            boolean condition = dateto == null ||
                                    (payment.getDate() != null && !payment.getDate().toLocalDate().isAfter(dateto));
                            System.out.println("DateTo-Filter (" + payment.getDate() + "): " + condition);
                            return condition;
                        })
                        .filter(payment -> {
                            boolean condition = amount == null ||
                                    (payment.getAmount() != null &&
                                            payment.getAmount() >= amount);
                            System.out.println("Amount-Filter (" + payment.getAmount() + "): " + condition);
                            return condition;
                        })
                .collect(Collectors.toList());

        return filteredPayments;
    }
}