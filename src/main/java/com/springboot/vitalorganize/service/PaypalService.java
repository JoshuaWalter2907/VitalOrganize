package com.springboot.vitalorganize.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paypal.api.payments.*;
import com.paypal.api.payments.Currency;
import com.paypal.base.rest.APIContext;
import com.paypal.base.rest.PayPalRESTException;
import com.springboot.vitalorganize.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PaypalService {

    public final APIContext apiContext;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;

    @Value("${paypal.client.id}")
    private static String clientId;

    @Value("${paypal.client.secret}")
    private static String clientSecret;

    @Value("${paypal.api.base-url}")
    private static String paypalBaseUrl;


    public String handlePaymentCreation(
            double amount,
            String currency,
            String method,
            String description,
            String cancelUrl,
            String successUrl
    ) throws PayPalRESTException {
        Payment payment = createPayment(amount, currency, method, "sale", description, cancelUrl, successUrl);

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
    public Zahlung addPayment(Zahlung payment) {
        // Letzte Buchung abrufen
        Optional<Zahlung> latestTransaction = paymentRepository.findLatestTransaction();

        double lastBalance = latestTransaction.map(Zahlung::getBalance).orElse(0.0);

        // Neuen Kontostand berechnen
        if (payment.getType() == PaymentType.EINZAHLEN) {
            payment.setBalance(lastBalance + payment.getAmount());
        } else if (payment.getType() == PaymentType.AUSZAHLEN) {
            payment.setBalance(lastBalance - payment.getAmount());
        }

        // Neue Buchung speichern
        return paymentRepository.save(payment);
    }


    public double getCurrentBalance() {
        return paymentRepository.findLatestTransaction()
                .map(Zahlung::getBalance)
                .orElse(0.0);
    }

    public static String getAccessToken() {
        String url = "https://api.sandbox.paypal.com/v1/oauth2/token";
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth("ARfr7uyYSZ9hmtos89I3skC0Fb9obRu7DThcOsZZyyMiTUG8s67m52cpe4accZ9aVSSKLQL0wfBPC6GP", "EKcu6J1jliBA_NK33drFcrIDAXDzAB6G4Wf9Kdsa0vPiTYOYQwNR8KFf2AbUOlkGEniYHEyTaHYicEqm");

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "client_credentials");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, request, Map.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            Map<String, String> body = response.getBody();
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
            String email, String method, String provider
    ) throws Exception {
        // Benutzer aktualisieren
        UserEntity user = userRepository.findByEmailAndProvider(email, provider);
        user.setRole("MEMBER");
        userRepository.save(user);

        // Zahlung initialisieren
        Zahlung zahlung = new Zahlung();
        zahlung.setDate(LocalDateTime.now());
        zahlung.setReason(description);
        zahlung.setAmount(Double.valueOf(amount));
        zahlung.setCurrency(currency);
        zahlung.setUser(user);

        if ("EINZAHLEN".equalsIgnoreCase(type)) {
            zahlung.setType(PaymentType.EINZAHLEN);
            Payment payment = executePayment(paymentId, payerId);
            if ("approved".equals(payment.getState())) {
                addPayment(zahlung);
                sendConfirmationEmail(email, amount, currency, method, description, type);
                return "/paypal";
            }
        } else if ("AUSZAHLEN".equalsIgnoreCase(type)) {
            double balance = getCurrentBalance();
            if (Double.valueOf(amount) > balance) {
                System.out.println("Error3");
                return "/paypal/error";
            } else {
                zahlung.setType(PaymentType.AUSZAHLEN);
                executePayout(receiverEmail, amount, currency);
                addPayment(zahlung);
                sendConfirmationEmail(email, amount, currency, method, description, type);
                return "/paypal";
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

}