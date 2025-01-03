package com.springboot.vitalorganize.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.paypal.api.payments.*;
import com.paypal.api.payments.Currency;
import com.paypal.base.rest.APIContext;
import com.paypal.base.rest.PayPalRESTException;
import com.springboot.vitalorganize.config.PayPalConfig;
import com.springboot.vitalorganize.model.*;
import com.springboot.vitalorganize.model.Payment;
import com.springboot.vitalorganize.service.repositoryhelper.FundRepositoryService;
import com.springboot.vitalorganize.service.repositoryhelper.PaymentRepositoryService;
import com.springboot.vitalorganize.service.repositoryhelper.SubscriptionRepositoryService;
import com.springboot.vitalorganize.service.repositoryhelper.UserRepositoryService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Service-Klasse zur Handhabung von PayPal-Zahlungen und Auszahlungen.
 * Diese Klasse bietet Methoden zur Erstellung von PayPal-Zahlungen, der Verarbeitung von Transaktionen,
 * der Verwaltung von Guthaben sowie der Durchführung von Auszahlungen.
 */
@Slf4j
@Service
@AllArgsConstructor
public class PaypalService {

    // Abhängigkeiten: Services und Repositorys, die für Zahlungen und Benutzerdaten benötigt werden
    private final SenderService senderService;
    private final PaymentRepositoryService paymentRepositoryService;
    private final UserRepositoryService userRepositoryService;
    private final FundRepositoryService fundRepositoryService;
    private final SubscriptionRepositoryService subscriptionRepositoryService;

    // Konfigurationsobjekte und PayPal API-Context
    public final APIContext apiContext;
    private final PayPalConfig payPalConfig;

    // PayPal-API URL für die Sandbox-Umgebung
    private static final String PAYPAL_API_URL = "https://api-m.sandbox.paypal.com/v1/billing/subscriptions";


    /**
     * Erstellt eine PayPal-Zahlung und leitet den Benutzer zur Zahlungsbestätigung weiter.
     *
     * Diese Methode stellt die grundlegende Logik für die Erstellung einer PayPal-Zahlung zur Verfügung und
     * leitet den Benutzer an die PayPal-Zahlungsseite weiter, um die Zahlung zu bestätigen.
     *
     * @param amount      Der Betrag der Zahlung in der angegebenen Währung.
     * @param type        Der Typ der Zahlung (z.B. Einzahlung, Auszahlung).
     * @param description Eine kurze Beschreibung der Zahlung, die dem Zahler angezeigt wird.
     * @param email       Die E-Mail-Adresse des Zahlers, die für die Zahlung verwendet wird.
     * @param fundid      Die ID des zugehörigen Fonds, auf den die Zahlung angewendet wird.
     * @param userId      Die ID des Benutzers, der die Zahlung initiiert.
     * @return Die URL zur PayPal-Zahlungsbestätigung, auf die der Benutzer weitergeleitet wird.
     * @throws PayPalRESTException Wenn bei der Kommunikation mit der PayPal-API ein Fehler auftritt.
     */
    public String createPaypalPayment(
            double amount,
            String type,
            String description,
            String email,
            Long fundid,
            Long userId
    ) throws PayPalRESTException {
        String currency = "EUR";  // Setzt die Währung für die Zahlung auf Euro
        String cancelUrl = "http://localhost:8080/fund/payinto/cancel";  // URL für den Fall einer abgebrochenen Zahlung
        String successUrl = "http://localhost:8080/fund/payinto/success";  // URL für den Fall einer erfolgreichen Zahlung

        // Aufruf der Hilfsmethode zur Erstellung der Zahlung und Rückgabe der Bestätigungs-URL
        return handlePaymentCreation(amount, currency, description, cancelUrl, successUrl);
    }

    /**
     * Handhabt die Erstellung einer PayPal-Zahlung und gibt die URL zur Bestätigung zurück.
     *
     * Diese Methode erstellt die Zahlung mit den übergebenen Parametern und gibt die URL zurück,
     * die der Benutzer zur Bestätigung der Zahlung besuchen muss.
     *
     * @param amount      Der Betrag der Zahlung in der angegebenen Währung.
     * @param currency    Die Währung der Zahlung (z.B. EUR).
     * @param description Eine kurze Beschreibung der Zahlung.
     * @param cancelUrl   Die URL, an die der Benutzer weitergeleitet wird, wenn die Zahlung abgebrochen wird.
     * @param successUrl  Die URL, an die der Benutzer weitergeleitet wird, wenn die Zahlung erfolgreich abgeschlossen wird.
     * @return Die URL zur PayPal-Zahlungsbestätigung.
     * @throws PayPalRESTException Wenn ein Fehler bei der Kommunikation mit der PayPal-API auftritt.
     */
    public String handlePaymentCreation(
            double amount,
            String currency,
            String description,
            String cancelUrl,
            String successUrl
    ) throws PayPalRESTException {
        // Erstelle die Zahlung mit den angegebenen Parametern
        com.paypal.api.payments.Payment payment = createPayment(amount, currency, "paypal", "sale", description, cancelUrl, successUrl);

        // Durchsuche die Links der erstellten Zahlung nach der Bestätigungs-URL
        for (Links link : payment.getLinks()) {
            if (link.getRel().equals("approval_url")) {
                return link.getHref();  // Gibt die URL zur Bestätigung der Zahlung zurück
            }
        }

        // Falls keine Bestätigungs-URL gefunden wurde, wirft die Methode eine Ausnahme
        throw new PayPalRESTException("Approval URL not found in the payment response.");
    }

    /**
     * Erzeugt eine neue PayPal-Zahlung.
     *
     * Diese Methode erstellt eine vollständige Zahlungsanforderung für PayPal, einschließlich des Betrags,
     * der Währung, der Zahlungsmethode und der Transaktionsdetails.
     *
     * @param total       Der Betrag der Zahlung.
     * @param currency    Die Währung der Zahlung.
     * @param method      Die Zahlungsmethode (z.B. "paypal").
     * @param intent      Der Zweck der Zahlung (z.B. "sale").
     * @param description Eine Beschreibung der Zahlung.
     * @param cancelUrl   Die URL für den Fall einer abgebrochenen Zahlung.
     * @param successUrl  Die URL für den Fall einer erfolgreichen Zahlung.
     * @return Ein Payment-Objekt, das die erstellte Zahlung darstellt.
     * @throws PayPalRESTException Wenn ein Fehler bei der Kommunikation mit der PayPal-API auftritt.
     */
    public com.paypal.api.payments.Payment createPayment(
            double total,
            String currency,
            String method,
            String intent,
            String description,
            String cancelUrl,
            String successUrl
    ) throws PayPalRESTException {
        // Erstelle den Betrag der Zahlung unter Verwendung der übergebenen Währung und des Betrags
        Amount amount = new Amount();
        amount.setCurrency(currency);
        amount.setTotal(String.format(Locale.forLanguageTag(currency), "%.2f", total));

        // Erstelle die Transaktion mit der angegebenen Beschreibung und dem Betrag
        Transaction transaction = new Transaction();
        transaction.setDescription(description);
        transaction.setAmount(amount);

        // Füge die Transaktion zu einer Liste hinzu
        List<Transaction> transactions = new ArrayList<>();
        transactions.add(transaction);

        // Erstelle den Zahler und setze die Zahlungsmethode auf "paypal"
        Payer payer = new Payer();
        payer.setPaymentMethod(method);

        // Erstelle das Payment-Objekt und setze die grundlegenden Zahlungsinformationen
        com.paypal.api.payments.Payment payment = new com.paypal.api.payments.Payment();
        payment.setIntent(intent);
        payment.setPayer(payer);
        payment.setTransactions(transactions);

        // Setze die URLs für den Fall von Erfolg oder Abbruch der Zahlung
        RedirectUrls redirectUrls = new RedirectUrls();
        redirectUrls.setCancelUrl(cancelUrl);
        redirectUrls.setReturnUrl(successUrl);

        payment.setRedirectUrls(redirectUrls);

        // Sende die Anfrage zur Erstellung der Zahlung
        return payment.create(apiContext);
    }

    /**
     * Führt eine bereits erstellte PayPal-Zahlung aus.
     *
     * Diese Methode führt die Zahlung aus, nachdem der Benutzer auf der PayPal-Seite zugestimmt hat,
     * und gibt das ausgeführte Payment-Objekt zurück.
     *
     * @param paymentId Die ID der zu ausführenden Zahlung.
     * @param payerId   Die ID des Zahlers.
     * @return Das ausgeführte Payment-Objekt.
     * @throws PayPalRESTException Wenn ein Fehler bei der Ausführung der Zahlung auftritt.
     */
    public com.paypal.api.payments.Payment executePayment(
            String paymentId,
            String payerId
    ) throws PayPalRESTException {

        // Initialisiere das Payment-Objekt mit der Payment-ID
        com.paypal.api.payments.Payment payment = new com.paypal.api.payments.Payment();
        payment.setId(paymentId);

        // Initialisiere PaymentExecution, um den Zahler zu setzen
        PaymentExecution paymentExecution = new PaymentExecution();
        paymentExecution.setPayerId(payerId);

        // Führt die Zahlung aus und gibt das Ergebnis zurück
        return payment.execute(apiContext, paymentExecution);
    }

    /**
     * Fügt eine Zahlung zur Datenbank hinzu und aktualisiert den Saldo.
     *
     * Diese Methode speichert eine neue Zahlung und berechnet den aktuellen Saldo, der auf der Art der Zahlung
     * basiert (Einzahlung oder Auszahlung).
     *
     * @param payment Die Payment-Instanz, die zur Datenbank hinzugefügt werden soll.
     * @param fundId  Die ID des Fonds, auf den die Zahlung angewendet wird.
     */
    public void addPayment(Payment payment, Long fundId) {
        // Holt die letzte Transaktion des Fonds, um den aktuellen Saldo zu ermitteln
        Payment latestTransaction = paymentRepositoryService.findLatestTransactionByFundId(fundId);
        double lastBalance = (latestTransaction == null) ? 0 : latestTransaction.getBalance();

        // Aktualisiert den Saldo basierend auf der Zahlungsart (Einzahlung oder Auszahlung)
        if (payment.getType() == PaymentType.EINZAHLEN) {
            payment.setBalance(lastBalance + payment.getAmount());
        } else if (payment.getType() == PaymentType.AUSZAHLEN) {
            payment.setBalance(lastBalance - payment.getAmount());
        }

        // Speichert die Zahlung in der Datenbank
        paymentRepositoryService.savePayment(payment);
    }

    /**
     * Gibt das aktuelle Guthaben zurück.
     *
     * Diese Methode holt das Guthaben des Fonds basierend auf der letzten Transaktion und gibt es zurück.
     * Wenn keine Transaktionen vorliegen, wird 0.0 zurückgegeben.
     *
     * @return Das aktuelle Guthaben des Fonds.
     */
    public double getCurrentBalance() {
        // Gibt das Guthaben der letzten Transaktion zurück, wenn vorhanden
        if(paymentRepositoryService.findLatestTransaction() == null)
            return 0.0;
        return paymentRepositoryService.findLatestTransaction().getBalance();
    }

    /**
     * Holt ein Access-Token von PayPal für API-Anfragen.
     *
     * Diese Methode ruft das Access-Token von PayPal ab, das für API-Anfragen verwendet wird.
     *
     * @return Das Access-Token für die Kommunikation mit der PayPal-API.
     */
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

        // Verarbeitet die Antwort und gibt das Access-Token zurück
        if (response.getStatusCode() == HttpStatus.OK) {
            Map<String, String> body = response.getBody();
            assert body != null;
            return body.get("access_token");
        } else {
            throw new RuntimeException("Failed to fetch access token: " + response.getStatusCode());
        }
    }

    /**
     * Führt eine Auszahlung an eine E-Mail-Adresse durch.
     *
     * Diese Methode führt eine Auszahlung durch, indem sie die PayPal-API verwendet, um eine Zahlung an eine
     * angegebene E-Mail-Adresse zu überweisen.
     *
     * @param recipientEmail Die E-Mail-Adresse des Empfängers der Auszahlung.
     * @param amount         Der Betrag der Auszahlung.
     * @param currency       Die Währung der Auszahlung.
     * @throws PayPalRESTException Wenn ein Fehler bei der Auszahlung auftritt.
     */
    public void executePayout(String recipientEmail, String amount, String currency) throws PayPalRESTException {
        // Erstelle Header für die Auszahlung
        PayoutSenderBatchHeader senderBatchHeader = new PayoutSenderBatchHeader();
        senderBatchHeader.setEmailSubject("You have a payout!");
        senderBatchHeader.setSenderBatchId(UUID.randomUUID().toString());

        // Erstelle die Auszahlungseinzelposten
        PayoutItem payoutItem = new PayoutItem();
        payoutItem.setRecipientType("EMAIL");
        payoutItem.setReceiver(recipientEmail);
        payoutItem.setAmount(new Currency(currency, amount));
        payoutItem.setNote("Thank you!");
        payoutItem.setSenderItemId(UUID.randomUUID().toString());

        List<PayoutItem> items = new ArrayList<>();
        items.add(payoutItem);

        // Erstelle die Auszahlung
        Payout payout = new Payout();
        payout.setSenderBatchHeader(senderBatchHeader);
        payout.setItems(items);

        // Holt das AccessToken für die API-Anfrage
        String accessToken = getAccessToken();

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        // JSON-Payload für die Auszahlung
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

        // Sende die Anfrage zur Auszahlung
        HttpEntity<String> requestEntity = new HttpEntity<>(jsonPayload, headers);
        String url = "https://api.sandbox.paypal.com/v1/payments/payouts";
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);

        // Überprüfe die Antwort und verarbeite die Auszahlung
        if (response.getStatusCode() == HttpStatus.CREATED) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                Map<String, Object> responseMap = objectMapper.readValue(response.getBody(), Map.class);
                Map<String, Object> batchHeader = (Map<String, Object>) responseMap.get("batch_header");
                String payoutBatchId = (String) batchHeader.get("payout_batch_id");
            } catch (Exception e) {
                throw new PayPalRESTException("Fehler beim Parsen der PayPal-Antwort: " + e.getMessage());
            }
        } else {
            throw new PayPalRESTException("Fehler bei der Auszahlung. HTTP Status: " + response.getStatusCode());
        }
    }


    @Transactional
    public void processPayment(
            String paymentId, String payerId, String type, String amount,
            String currency, String description, String receiverEmail,
            String email, String provider, Long id, Long fundId
    ) throws Exception {
        // Abruf des Benutzers anhand der E-Mail und des Anbieters (z. B. GitHub oder anderer Anbieter)
        UserEntity user = userRepositoryService.findByEmailAndProvider(email, provider);
        if (user.getProvider().equals("github"))
            email = user.getSendtoEmail();  // Falls der Anbieter GitHub ist, wird die alternative E-Mail verwendet

        // Abruf des Fundes anhand der Fund-ID
        FundEntity fund = fundRepositoryService.findFundById(fundId);

        // Erstellen einer neuen Zahlung (Payment)
        Payment zahlung = new Payment();
        zahlung.setDate(LocalDateTime.now());  // Setzt das aktuelle Datum und die Uhrzeit als Zahlungsdatum
        zahlung.setReason(description);  // Setzt die Zahlungsbeschreibung
        zahlung.setAmount(Double.valueOf(amount));  // Setzt den Betrag
        zahlung.setCurrency(currency);  // Setzt die Währung
        zahlung.setUser(user);  // Verknüpft die Zahlung mit dem Benutzer
        zahlung.setFund(fund);  // Verknüpft die Zahlung mit dem Fund

        // Bearbeitung des Zahlungstyps (Einzahlung oder Auszahlung)
        if ("EINZAHLEN".equalsIgnoreCase(type)) {
            zahlung.setType(PaymentType.EINZAHLEN);  // Setzt den Zahlungstyp auf Einzahlung
            com.paypal.api.payments.Payment payment = executePayment(paymentId, payerId);  // Führt die PayPal-Zahlung aus
            if ("approved".equals(payment.getState())) {  // Überprüft, ob die Zahlung genehmigt wurde
                addPayment(zahlung, fundId);  // Fügt die Zahlung der Datenbank hinzu
                senderService.sendConfirmationEmail(email, amount, currency, "paypal", description, type);  // Bestätigungsmail senden
                return;  // Abschluss der Verarbeitung für Einzahlung
            }
        } else if ("AUSZAHLEN".equalsIgnoreCase(type)) {
            // Bearbeitung des Falles für Auszahlungen
            double balance = getCurrentBalance();  // Abrufen des aktuellen Kontostands
            if (Double.parseDouble(amount) > balance) {
                return;  // Wenn der Betrag größer als der Kontostand ist, wird die Auszahlung abgebrochen
            } else {
                zahlung.setType(PaymentType.AUSZAHLEN);  // Setzt den Zahlungstyp auf Auszahlung
                executePayout(receiverEmail, amount, currency);  // Führt die PayPal-Auszahlung aus
                addPayment(zahlung, fundId);  // Fügt die Zahlung der Datenbank hinzu
                senderService.sendConfirmationEmail(email, amount, currency, "paypal", description, type);  // Bestätigungsmail senden
                return;  // Abschluss der Verarbeitung für Auszahlung
            }
        }
        throw new RuntimeException("Invalid payment type");  // Wirft eine Ausnahme, wenn der Zahlungstyp ungültig ist
    }

    public String createSubscription(String planId, UserEntity user) {
        try {
            // Abrufen des Access Tokens für die PayPal-API
            String accessToken = getAccessToken();

            // Erstellen der Abonnementanfrage
            String subscriptionRequest = createSubscriptionRequest(planId, user);

            // Erstellen einer HTTP-Anfrage an die PayPal-API zur Erstellung des Abonnements
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);  // Setzt das Access Token im Header
            headers.setContentType(MediaType.APPLICATION_JSON);  // Setzt den Content-Type auf JSON
            HttpEntity<String> entity = new HttpEntity<>(subscriptionRequest, headers);

            // Senden der Anfrage an die PayPal-API und Abrufen der Antwort
            ResponseEntity<String> response = restTemplate.exchange(PAYPAL_API_URL, HttpMethod.POST, entity, String.class);

            // Extrahieren der Genehmigungs-URL aus der Antwort
            return extractApprovalUrl(response.getBody());
        } catch (Exception e) {
            log.atTrace().log(e.getMessage());  // Loggt den Fehler, wenn die Erstellung des Abonnements fehlschlägt
            return null;  // Gibt null zurück, falls ein Fehler auftritt
        }
    }

    private String createSubscriptionRequest(String planId, UserEntity user) {
        // Bereitet die JSON-Daten für die Abonnementanfrage vor
        String email = user.getEmail();
        if (user.getProvider().equals("github"))
            email = user.getSendtoEmail();  // Verwendet die alternative E-Mail, wenn der Anbieter GitHub ist

        String returnUrl = "http://localhost:8080/subscription-success?userId=" + user.getId();
        String cancelUrl = "http://localhost:8080/subscription-cancel?userId=" + user.getId();

        // JSON-Daten für die Anfrage
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
            // Verarbeitet die PayPal-Antwort, um die Genehmigungs-URL zu extrahieren
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(responseBody);
            JsonNode linksNode = rootNode.get("links");

            // Durchsucht die Links nach der Genehmigungs-URL
            if (linksNode != null) {
                for (JsonNode link : linksNode) {
                    String rel = link.get("rel").asText();
                    if ("approve".equalsIgnoreCase(rel)) {
                        return link.get("href").asText();  // Rückgabe der Genehmigungs-URL
                    }
                }
            }
        } catch (Exception e) {
            log.atTrace().log(e.getMessage());  // Loggt den Fehler, falls das Parsen fehlschlägt
        }
        return null;  // Gibt null zurück, wenn keine Genehmigungs-URL gefunden wurde
    }

    public String getPayerIdFromSubscription(String subscriptionId) {
        try {
            // Abrufen des Access Tokens für die PayPal-API
            String accessToken = getAccessToken();

            // Abrufen der Abonnementdetails von PayPal
            String subscriptionUrl = "https://api-m.sandbox.paypal.com/v1/billing/subscriptions/" + subscriptionId;
            HttpURLConnection httpConn = (HttpURLConnection) new URL(subscriptionUrl).openConnection();
            httpConn.setRequestMethod("GET");
            httpConn.setRequestProperty("Authorization", "Bearer " + accessToken);  // Setzt das Access Token im Header
            httpConn.setRequestProperty("Content-Type", "application/json");

            // Liest die Antwort der PayPal-API
            InputStream responseStream = httpConn.getResponseCode() / 100 == 2
                    ? httpConn.getInputStream()
                    : httpConn.getErrorStream();
            Scanner s = new Scanner(responseStream).useDelimiter("\\A");
            String response = s.hasNext() ? s.next() : "";

            // Parsen der Antwort und Abrufen der Payer ID
            JsonObject jsonObject = JsonParser.parseString(response).getAsJsonObject();
            String payerId = jsonObject.getAsJsonObject("subscriber").get("payer_id").getAsString();

            return Objects.requireNonNullElse(payerId, "PayerID konnte nicht abgerufen werden.");
        } catch (Exception e) {
            log.atTrace().log(e.getMessage());  // Loggt den Fehler, falls die Payer ID nicht abgerufen werden kann
            return "Fehler bei der Abfrage der PayerID";  // Gibt eine Fehlermeldung zurück
        }
    }

    public String confirmSubscription(String subscriptionId, String payerId) {
        // Bestätigt das Abonnement bei PayPal und aktivieren das Abonnement, falls es noch nicht aktiviert wurde
        String accessToken = getAccessToken();  // Abrufen des Access Tokens

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);  // Setzt das Access Token im Header
        headers.setContentType(MediaType.APPLICATION_JSON);  // Setzt den Content-Type auf JSON

        // Abrufen des aktuellen Status des Abonnements von PayPal
        String getSubscriptionUrl = "https://api-m.sandbox.paypal.com/v1/billing/subscriptions/" + subscriptionId;
        ResponseEntity<String> response = restTemplate.exchange(getSubscriptionUrl, HttpMethod.GET, new HttpEntity<>(headers), String.class);

        // Überprüft den aktuellen Status des Abonnements
        String subscriptionStatus = parseSubscriptionStatus(response.getBody());

        // Falls das Abonnement nicht "SUSPENDED" (unterbrochen) ist, wird es vor der Aktivierung pausiert
        if (!"SUSPENDED".equalsIgnoreCase(subscriptionStatus)) {
            String suspendUrl = "https://api-m.sandbox.paypal.com/v1/billing/subscriptions/" + subscriptionId + "/suspend";
            response = restTemplate.exchange(suspendUrl, HttpMethod.POST, new HttpEntity<>(headers), String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                return "Error suspending subscription.";  // Fehlermeldung, falls das Pausieren fehlschlägt
            }
        }

        // Aktivierung des Abonnements
        String activateUrl = "https://api-m.sandbox.paypal.com/v1/billing/subscriptions/" + subscriptionId + "/activate";
        String body = "{ \"payer_id\": \"" + payerId + "\" }";  // Falls PayerId erforderlich ist
        HttpEntity<String> entity = new HttpEntity<>(body, headers);
        response = restTemplate.exchange(activateUrl, HttpMethod.POST, entity, String.class);

        // Rückgabe des Ergebnisses basierend auf der Antwort von PayPal
        if (response.getStatusCode().is2xxSuccessful()) {
            return "approved";  // Erfolgreiche Aktivierung
        } else {
            return "failed";  // Fehlgeschlagene Aktivierung
        }
    }

    private String parseSubscriptionStatus(String responseBody) {
        try {
            // Parsen der Antwort von PayPal, um den Abonnementstatus zu extrahieren
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(responseBody);
            return rootNode.path("status").asText();  // Gibt den Abonnementstatus zurück
        } catch (Exception e) {
            log.atTrace().log(e.getMessage());  // Loggt den Fehler, falls das Parsen fehlschlägt
            return "Error parsing subscription status";  // Gibt eine Fehlermeldung zurück
        }
    }

    public boolean cancelSubscription(UserEntity user, String subscriptionId) {
        // Überprüft, ob das Abonnement bereits gekündigt wurde
        if(user.getLatestSubscription().getStatus().equals("CANCELLED")) {
            return true;
        }

        // Wenn das Abonnement existiert, wird es sowohl bei PayPal als auch lokal gekündigt
        if (user.getLatestSubscription() != null) {
            SubscriptionEntity subscription = user.getLatestSubscription();

            boolean isPaypalCancelled = cancelPaypalSubscription(subscription.getSubscriptionId());

            if (!isPaypalCancelled) {
                return false;  // Gibt false zurück, wenn die PayPal-Kündigung fehlschlägt
            }

            // Setzt den Status auf "CANCELLED" und speichert die Änderung in der Datenbank
            subscription.setStatus("CANCELLED");
            subscriptionRepositoryService.saveSubscription(subscription);
            return true;
        }

        return false; // Gibt false zurück, wenn kein Abonnement gefunden wurde
    }

    private boolean cancelPaypalSubscription(String paypalSubscriptionId) {
        try {
            // Abrufen des Access Tokens
            String accessToken = getAccessToken();

            // Senden einer Anfrage an PayPal, um das Abonnement zu kündigen
            String url = PAYPAL_API_URL + "/" + paypalSubscriptionId + "/cancel";
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);  // Setzt das Access Token im Header
            headers.setContentType(MediaType.APPLICATION_JSON);  // Setzt den Content-Type auf JSON

            // Anfrage zum Kündigen des Abonnements
            String body = "{ \"reason\": \"User requested cancellation.\" }";
            HttpEntity<String> entity = new HttpEntity<>(body, headers);

            // Senden der Anfrage und Rückgabe des Ergebnisses
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            return response.getStatusCode() == HttpStatus.NO_CONTENT;  // Erfolgreiche Kündigung wird mit "NO_CONTENT" bestätigt
        } catch (Exception e) {
            log.atTrace().log(e.getMessage());  // Loggt den Fehler, falls die Kündigung fehlschlägt
            return false;  // Rückgabe von false bei einem Fehler
        }
    }


    /**
     * Pauses the subscription of a given user by suspending the corresponding PayPal subscription
     * and updating the subscription status to "SUSPENDED" in the system.
     *
     * @param user The user whose subscription is to be paused.
     * @return true if the subscription was successfully paused, false otherwise.
     */
    public boolean pauseSubscription(UserEntity user) {
        // Check if the subscription is already canceled, in which case no action is required.
        if(user.getLatestSubscription().getStatus().equals("CANCELLED")) {
            return true;
        }

        // Proceed only if there is an active subscription
        if (user.getLatestSubscription() != null) {
            SubscriptionEntity subscription = user.getLatestSubscription();

            // Attempt to pause the PayPal subscription
            boolean isPaypalCancelled = pausePayPalSubscription(subscription.getSubscriptionId());

            // If the PayPal suspension failed, return false
            if (!isPaypalCancelled) {
                return false;
            }

            // Update the subscription status to "SUSPENDED" and save it
            subscription.setStatus("SUSPENDED");
            subscriptionRepositoryService.saveSubscription(subscription);
            return true;
        }

        // Return false if no subscription exists for the user
        return false;
    }


    /**
     * Pauses a PayPal subscription by sending a suspension request to PayPal's API.
     *
     * @param subscriptionId The ID of the PayPal subscription to pause.
     * @return true if the subscription was successfully paused, false otherwise.
     */
    public boolean pausePayPalSubscription(String subscriptionId) {
        try {
            // Retrieve the access token needed for authentication with PayPal's API
            String accessToken = getAccessToken();

            // Set up the necessary HTTP headers and the request body
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + accessToken);

            String requestBody = """
                {
                    "reason": "User requested to pause the subscription"
                }
                """;

            // Prepare the request entity with headers and body
            HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);

            // Send the request to the PayPal API to suspend the subscription
            ResponseEntity<String> response = restTemplate.exchange(
                    PAYPAL_API_URL + "/" + subscriptionId + "/suspend",
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            // Return true if the response status code indicates success (2xx)
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            // Log any exception and return false in case of an error
            log.atTrace().log(e.getMessage());
            return false;
        }
    }


    /**
     * Resumes an already paused subscription by reactivating the corresponding PayPal subscription
     * and updating the subscription status to "ACTIVE" in the system.
     *
     * @param user The user whose subscription is to be resumed.
     * @param subscriptionId The ID of the subscription to resume.
     * @return true if the subscription was successfully resumed, false otherwise.
     */
    public boolean resumeSubscription(UserEntity user, String subscriptionId) {
        // Check if the subscription is already active, in which case no action is required.
        if(user.getLatestSubscription().getStatus().equals("ACTIVE")) {
            return true;
        }

        // Proceed only if there is a subscription to resume
        if (user.getLatestSubscription() != null) {
            SubscriptionEntity subscription = user.getLatestSubscription();

            // Attempt to resume the PayPal subscription
            boolean isPaypalResumed = resumePayPalSubscription(subscriptionId);

            // If the PayPal resumption failed, return false
            if (!isPaypalResumed) {
                return false;
            }

            // Update the subscription status to "ACTIVE" and save it
            subscription.setStatus("ACTIVE");
            subscriptionRepositoryService.saveSubscription(subscription);
            return true;
        }

        // Return false if no subscription exists for the user
        return false;
    }


    /**
     * Resumes a PayPal subscription by sending an activation request to PayPal's API.
     *
     * @param subscriptionId The ID of the PayPal subscription to activate.
     * @return true if the subscription was successfully resumed, false otherwise.
     */
    public boolean resumePayPalSubscription(String subscriptionId) {
        try {
            // Retrieve the access token needed for authentication with PayPal's API
            String accessToken = getAccessToken();

            // Set up the necessary HTTP headers and the request body
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + accessToken);

            String requestBody = """
            {
                "reason": "User requested to resume the subscription"
            }
            """;

            // Prepare the request entity with headers and body
            HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);

            // Send the request to the PayPal API to activate the subscription
            ResponseEntity<String> response = restTemplate.exchange(
                    PAYPAL_API_URL + "/" + subscriptionId + "/activate",
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            // Return true if the response status code indicates success (2xx)
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            // Log any exception and return false in case of an error
            log.atTrace().log(e.getMessage());
            return false;
        }
    }


    /**
     * Retrieves a list of transactions for a specific PayPal subscription within a defined time range.
     *
     * @param subscriptionId The ID of the PayPal subscription to fetch transactions for.
     * @return A list of transactions associated with the given subscription, or null if no transactions are found.
     */
    public List<TransactionSubscription> getTransactionsForSubscription(String subscriptionId) {
        try {
            // Retrieve the access token needed for authentication with PayPal's API
            String accessToken = getAccessToken();

            // Define the time range for transactions
            String startTime = "2023-12-01T00:00:00Z";
            String endTime = "2025-12-27T23:59:59Z";

            // Create the endpoint URL with query parameters for the time range
            String url = UriComponentsBuilder.fromHttpUrl(PAYPAL_API_URL + "/" + subscriptionId + "/transactions")
                    .queryParam("start_time", startTime)
                    .queryParam("end_time", endTime)
                    .toUriString();

            // Set up the request headers and send the GET request to PayPal's API
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            // Receive the response from PayPal's API
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            // If the response is successful, parse and return the transactions
            if (response.getStatusCode().is2xxSuccessful()) {
                String responseBody = response.getBody();
                return parseTransactions(responseBody);
            } else {
                return null;
            }
        } catch (Exception e) {
            // Log any exception and return null in case of an error
            log.atTrace().log(e.getMessage());
            return null;
        }
    }


    /**
     * Parses a JSON response from the PayPal API to extract transaction details.
     *
     * @param jsonResponse The JSON response string from the PayPal API.
     * @return A list of `TransactionSubscription` objects parsed from the response, or an empty list in case of an error.
     */
    private List<TransactionSubscription> parseTransactions(String jsonResponse) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule()); // Register module for Java 8 time types

        try {
            // Parse the JSON response to extract the transaction data
            JsonNode jsonNode = objectMapper.readTree(jsonResponse);
            JsonNode transactionsNode = jsonNode.path("transactions");

            // Deserialize the transaction node into a list of TransactionSubscription objects
            return objectMapper.readerForListOf(TransactionSubscription.class)
                    .readValue(transactionsNode);
        } catch (Exception e) {
            // Log any exception and return an empty list in case of an error
            log.atTrace().log(e.getMessage());
            return new ArrayList<>();
        }
    }


    /**
     * Filters a list of transactions based on the given parameters, such as username, date range, and amount.
     *
     * @param transactions The list of transactions to filter.
     * @param username The username to match against the payer's email.
     * @param datefrom The start date of the transaction range.
     * @param dateto The end date of the transaction range.
     * @param amount The minimum transaction amount.
     * @return A filtered list of transactions based on the given criteria.
     */
    public List<TransactionSubscription> filterTransactions(
            List<TransactionSubscription> transactions,
            String username,
            LocalDate datefrom,
            LocalDate dateto,
            Long amount) {

        return transactions.stream()
                .filter(transaction -> username == null || username.isEmpty() ||
                        (transaction.getPayerEmail() != null &&
                                transaction.getPayerEmail().toLowerCase().contains(username.toLowerCase())))
                .filter(transaction -> datefrom == null ||
                        (transaction.getTime() != null && !transaction.getTime().toLocalDate().isBefore(datefrom)))
                .filter(transaction -> dateto == null ||
                        (transaction.getTime() != null && !transaction.getTime().toLocalDate().isAfter(dateto)))
                .filter(transaction -> amount == null ||
                        (transaction.getAmountWithBreakdown() != null &&
                                transaction.getAmountWithBreakdown().getGrossAmount() != null &&
                                Double.parseDouble(transaction.getAmountWithBreakdown().getGrossAmount().getValue()) >= amount))
                .collect(Collectors.toList());
    }



    /**
     * Filters a list of payments based on the provided criteria, including username, reason, date range, and amount.
     *
     * @param payments The list of payments to filter.
     * @param username The username to match against the user's username.
     * @param reason The reason for the payment to match.
     * @param datefrom The start date of the payment range.
     * @param dateto The end date of the payment range.
     * @param amount The minimum payment amount.
     * @return A filtered list of payments based on the given criteria.
     */
    public List<Payment> filterPayments(List<Payment> payments,
                                        String username,
                                        String reason,
                                        LocalDate datefrom,
                                        LocalDate dateto,
                                        Long amount) {

        return payments.stream()
                .filter(payment -> {
                    String usernameToCheck = (payment.getUser() != null) ? payment.getUser().getUsername() : "Deleted User";
                    return username == null || username.isEmpty() ||
                            usernameToCheck.toLowerCase().contains(username.toLowerCase());
                })
                .filter(payment -> reason == null || reason.isEmpty() ||
                        (payment.getReason() != null &&
                                payment.getReason().toLowerCase().contains(reason.toLowerCase())))
                .filter(payment -> datefrom == null ||
                        (payment.getDate() != null && !payment.getDate().toLocalDate().isBefore(datefrom)))
                .filter(payment -> dateto == null ||
                        (payment.getDate() != null && !payment.getDate().toLocalDate().isAfter(dateto)))
                .filter(payment -> amount == null ||
                        (payment.getAmount() != null &&
                                payment.getAmount() >= amount))
                .collect(Collectors.toList());
    }

}