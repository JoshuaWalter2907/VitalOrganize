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
import com.springboot.vitalorganize.entity.*;
import com.springboot.vitalorganize.entity.Fund_Payments.FundEntity;
import com.springboot.vitalorganize.entity.Fund_Payments.PaymentEntity;
import com.springboot.vitalorganize.entity.Fund_Payments.PaymentTypeEnumeration;
import com.springboot.vitalorganize.entity.Fund_Payments.TransactionSubscriptionEntity;
import com.springboot.vitalorganize.entity.Profile_User.UserEntity;
import com.springboot.vitalorganize.model.Fund_Payment.PaymentInformationSessionDTO;
import com.springboot.vitalorganize.model.Fund_Payment.PaymentSuccessRequestDTO;
import com.springboot.vitalorganize.repository.FundRepository;
import com.springboot.vitalorganize.repository.PaymentRepository;
import com.springboot.vitalorganize.repository.SubscriptionRepository;
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
 */
@Slf4j
@Service
@AllArgsConstructor
public class PaypalService {


    private final PaymentRepository paymentRepository;
    private final FundRepository fundRepository;
    private final SubscriptionRepository subscriptionRepository;

    private final SenderService senderService;
    public final APIContext apiContext;
    private final PayPalConfig payPalConfig;

    private static final String PAYPAL_API_URL = "https://api-m.sandbox.paypal.com/v1/billing/subscriptions";


    /**
     * Erstelle eine Zahlung
     * @param paymentInformationSessionDTO Benötigte Informationen dafür
     * @return Url nach PayPal Ausführung
     * @throws PayPalRESTException Wenn ein Fehler bei der Kommunikation mit der PayPal-API auftritt
     */
    public String createPaypalPayment(
            PaymentInformationSessionDTO paymentInformationSessionDTO
    ) throws PayPalRESTException {
        String currency = "EUR";
        String cancelUrl = "http://localhost:8080/fund/payinto/cancel";
        String successUrl = "http://localhost:8080/fund/payinto/success";

        return handlePaymentCreation(Double.parseDouble(paymentInformationSessionDTO.getAmount()), currency, paymentInformationSessionDTO.getDescription(), cancelUrl, successUrl);
    }

    /**
     * Handhabt die Erstellung einer PayPal-Zahlung und gibt die URL zur Bestätigung zurück
     * @param amount      Der Betrag der Zahlung
     * @param currency    Die Währung der Zahlung
     * @param description Eine kurze Beschreibung
     * @param cancelUrl   Die URL, an die der Benutzer weitergeleitet wird, wenn die Zahlung abgebrochen wird
     * @param successUrl  Die URL, an die der Benutzer weitergeleitet wird, wenn die Zahlung erfolgreich abgeschlossen wird
     * @return Die URL zur PayPal-Zahlungsbestätigung.
     * @throws PayPalRESTException Wenn ein Fehler bei der Kommunikation mit der PayPal-API auftritt
     */
    public String handlePaymentCreation(
            double amount,
            String currency,
            String description,
            String cancelUrl,
            String successUrl
    ) throws PayPalRESTException {
        com.paypal.api.payments.Payment payment = createPayment(amount, currency, "paypal", "sale", description, cancelUrl, successUrl);

        for (Links link : payment.getLinks()) {
            if (link.getRel().equals("approval_url")) {
                return link.getHref();
            }
        }

        throw new PayPalRESTException("Approval URL not found in the payment response.");
    }

    /**
     * Erzeugt eine neue PayPal-Zahlung
     * @param total       Der Betrag
     * @param currency    Die Währung
     * @param method      Die Zahlungsmethode
     * @param intent      Der Zweck der Zahlung
     * @param description Eine Beschreibung der Zahlung
     * @param cancelUrl   Die URL für den Fall einer abgebrochenen Zahlung
     * @param successUrl  Die URL für den Fall einer erfolgreichen Zahlung
     * @return Ein Payment-Objekt, das die erstellte Zahlung darstellt
     * @throws PayPalRESTException Wenn ein Fehler bei der Kommunikation mit der PayPal-API auftritt
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

        com.paypal.api.payments.Payment payment = new com.paypal.api.payments.Payment();
        payment.setIntent(intent);
        payment.setPayer(payer);
        payment.setTransactions(transactions);

        RedirectUrls redirectUrls = new RedirectUrls();
        redirectUrls.setCancelUrl(cancelUrl);
        redirectUrls.setReturnUrl(successUrl);

        payment.setRedirectUrls(redirectUrls);

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
     * Fügt eine Zahlung zur Datenbank hinzu und aktualisiert den Saldo
     * @param payment Die Payment-Instanz, die zur Datenbank hinzugefügt werden soll
     * @param fundId  Die ID des Fonds, auf den die Zahlung angewendet wird
     */
    public void addPayment(PaymentEntity payment, Long fundId) {
        PaymentEntity latestTransaction = paymentRepository.findFirstByFundIdOrderByDateDesc(fundId);
        double lastBalance = (latestTransaction == null) ? 0 : latestTransaction.getBalance();

        if (payment.getType() == PaymentTypeEnumeration.EINZAHLEN) {
            payment.setBalance(lastBalance + payment.getAmount());
        } else if (payment.getType() == PaymentTypeEnumeration.AUSZAHLEN) {
            payment.setBalance(lastBalance - payment.getAmount());
        }

        paymentRepository.save(payment);
    }

    /**
     * Gibt das aktuelle Guthaben zurück
     * @return Das aktuelle Guthaben des Fonds
     */
    public double getCurrentBalance() {
        PaymentEntity latestTransaction = paymentRepository.findFirstByOrderByDateDesc();

        if (latestTransaction == null) {
            return 0.0;
        }

        return latestTransaction.getBalance();
    }

    /**
     * Holt ein Access-Token von PayPal für API-Anfragen.
     * @return Das Access-Token für die Kommunikation mit der PayPal-API
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

        if (response.getStatusCode() == HttpStatus.OK) {
            Map<String, String> body = response.getBody();
            assert body != null;
            return body.get("access_token");
        } else {
            throw new RuntimeException("Failed to fetch access token: " + response.getStatusCode());
        }
    }

    /**
     * Führt eine Auszahlung an eine E-Mail-Adresse durch
     * @param recipientEmail Die E-Mail-Adresse des Empfängers
     * @param amount         Der Betrag der Auszahlung
     * @param currency       Die Währung der Auszahlung
     * @throws PayPalRESTException Wenn ein Fehler bei der Auszahlung auftritt
     */
    public void executePayout(String recipientEmail, String amount, String currency) throws PayPalRESTException {
        PayoutSenderBatchHeader senderBatchHeader = new PayoutSenderBatchHeader();
        senderBatchHeader.setEmailSubject("You have a payout!");
        senderBatchHeader.setSenderBatchId(UUID.randomUUID().toString());

        PayoutItem payoutItem = new PayoutItem();
        payoutItem.setRecipientType("EMAIL");
        payoutItem.setReceiver(recipientEmail);
        payoutItem.setAmount(new Currency(currency, amount));
        payoutItem.setNote("Thank you!");
        payoutItem.setSenderItemId(UUID.randomUUID().toString());

        List<PayoutItem> items = new ArrayList<>();
        items.add(payoutItem);

        Payout payout = new Payout();
        payout.setSenderBatchHeader(senderBatchHeader);
        payout.setItems(items);

        String accessToken = getAccessToken();

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

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
    }


    /**
     * Nimmt die Zahlungaufforderung vom Controller und führt entweder eine AUSZAHLUNG oder eine EINZAHLUNG aus
     * @param user der die Zahlung machen möchte
     * @param paymentInformationSessionDTO Informationen der eigentlichen Zahlung
     * @param paymentSuccessRequestDTO Enthählt PaymentId und PayerId
     * @throws Exception Falls mit der PayPal API etwas schief gelaufen ist
     */
    @Transactional
    public void processPayment(UserEntity user, PaymentInformationSessionDTO paymentInformationSessionDTO, PaymentSuccessRequestDTO paymentSuccessRequestDTO
    ) throws Exception {
        String email = user.getProvider().equals("github") ? user.getSendtoEmail() : user.getEmail();
        FundEntity fund = fundRepository.findById(paymentInformationSessionDTO.getFundid()).orElse(null);

        PaymentEntity paymentEntity = new PaymentEntity();
        paymentEntity.setDate(LocalDateTime.now());
        paymentEntity.setReason(paymentInformationSessionDTO.getDescription());
        paymentEntity.setAmount(Double.valueOf(paymentInformationSessionDTO.getAmount()));
        paymentEntity.setCurrency("EUR");
        paymentEntity.setUser(user);
        paymentEntity.setFund(fund);

        if ("EINZAHLEN".equalsIgnoreCase(paymentInformationSessionDTO.getType())) {
            paymentEntity.setType(PaymentTypeEnumeration.EINZAHLEN);
            Payment payment = executePayment(paymentSuccessRequestDTO.getPaymentId(), paymentSuccessRequestDTO.getPayerID());
            if ("approved".equals(payment.getState())) {
                addPayment(paymentEntity, paymentInformationSessionDTO.getFundid());
                senderService.sendConfirmationEmail(email, paymentInformationSessionDTO.getAmount(), "EUR", "paypal", paymentInformationSessionDTO.getDescription(), paymentInformationSessionDTO.getType());
                return;
            }
        } else if ("AUSZAHLEN".equalsIgnoreCase(paymentInformationSessionDTO.getType())) {
            double balance = getCurrentBalance();
            if (Double.parseDouble(paymentInformationSessionDTO.getAmount()) > balance) {
                return;
            } else {
                paymentEntity.setType(PaymentTypeEnumeration.AUSZAHLEN);
                executePayout(paymentInformationSessionDTO.getReceiverEmail(), paymentInformationSessionDTO.getAmount(), "EUR");
                addPayment(paymentEntity, paymentInformationSessionDTO.getFundid());
                senderService.sendConfirmationEmail(email, paymentInformationSessionDTO.getAmount(), "EUR", "paypal", paymentInformationSessionDTO.getDescription(), paymentInformationSessionDTO.getType());
                return;
            }
        }
        throw new RuntimeException("Invalid payment type");
    }

    /**
     * Zuständig um eine Premium Subscription für den Benutzer abzuschließen
     * @param planId Der Plan, der die subscription Daten enthält
     * @param user Der Benutzer, der das Abo machen möchte
     * @return Url, auf die redirected wird
     */
    public String createSubscription(String planId, UserEntity user) {
        try {
            String accessToken = getAccessToken();

            String subscriptionRequest = createSubscriptionRequest(planId, user);

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(subscriptionRequest, headers);

            ResponseEntity<String> response = restTemplate.exchange(PAYPAL_API_URL, HttpMethod.POST, entity, String.class);

            return extractApprovalUrl(response.getBody());
        } catch (Exception e) {
            log.atTrace().log(e.getMessage());
            return null;
        }
    }

    /**
     * Erstellt den Request für die PayPal API
     * @param planId Der Plan, der die subscription Daten enthält
     * @param user Der Benutzer, der das Abo machen möchte
     * @return Anfrage im JSON format
     */
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

    /**
     * Holt sich die Bestätigung von der PayPal API aus der Anfrage
     * @param responseBody JSON Antwort von PayPal
     * @return Genehmigungs-Url oder null
     */
    private String extractApprovalUrl(String responseBody) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(responseBody);
            JsonNode linksNode = rootNode.get("links");

            if (linksNode != null) {
                for (JsonNode link : linksNode) {
                    String rel = link.get("rel").asText();
                    if ("approve".equalsIgnoreCase(rel)) {
                        return link.get("href").asText();
                    }
                }
            }
        } catch (Exception e) {
            log.atTrace().log(e.getMessage());
        }
        return null;
    }

    /**
     * Holt sich die PayerId aus einer aktiven Subscription
     * @param subscriptionId Benötigt Id für die aktive Subscription
     * @return die PayerId
     */
    public String getPayerIdFromSubscription(String subscriptionId) {
        try {
            String accessToken = getAccessToken();

            String subscriptionUrl = "https://api-m.sandbox.paypal.com/v1/billing/subscriptions/" + subscriptionId;
            HttpURLConnection httpConn = (HttpURLConnection) new URL(subscriptionUrl).openConnection();
            httpConn.setRequestMethod("GET");
            httpConn.setRequestProperty("Authorization", "Bearer " + accessToken);  // Setzt das Access Token im Header
            httpConn.setRequestProperty("Content-Type", "application/json");

            InputStream responseStream = httpConn.getResponseCode() / 100 == 2
                    ? httpConn.getInputStream()
                    : httpConn.getErrorStream();
            Scanner s = new Scanner(responseStream).useDelimiter("\\A");
            String response = s.hasNext() ? s.next() : "";

            JsonObject jsonObject = JsonParser.parseString(response).getAsJsonObject();
            String payerId = jsonObject.getAsJsonObject("subscriber").get("payer_id").getAsString();

            return Objects.requireNonNullElse(payerId, "PayerID konnte nicht abgerufen werden.");
        } catch (Exception e) {
            log.atTrace().log(e.getMessage());
            return "Fehler bei der Abfrage der PayerID";
        }
    }

    /**
     * Bestätigt das Abonnement bei PayPal und aktivieren das Abonnement, falls es noch nicht aktiviert wurde
     * @param subscriptionId Benötigt Id für die aktive Subscription
     * @param payerId Benötigt PayerId für die aktive Subscription
     * @return Ist die Subscription bestätigt
     */
    public String confirmSubscription(String subscriptionId, String payerId) {
        String accessToken = getAccessToken();

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        String getSubscriptionUrl = "https://api-m.sandbox.paypal.com/v1/billing/subscriptions/" + subscriptionId;
        ResponseEntity<String> response = restTemplate.exchange(getSubscriptionUrl, HttpMethod.GET, new HttpEntity<>(headers), String.class);

        String subscriptionStatus = parseSubscriptionStatus(response.getBody());

        if (!"SUSPENDED".equalsIgnoreCase(subscriptionStatus)) {
            String suspendUrl = "https://api-m.sandbox.paypal.com/v1/billing/subscriptions/" + subscriptionId + "/suspend";
            response = restTemplate.exchange(suspendUrl, HttpMethod.POST, new HttpEntity<>(headers), String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                return "Error suspending subscription.";
            }
        }

        String activateUrl = "https://api-m.sandbox.paypal.com/v1/billing/subscriptions/" + subscriptionId + "/activate";
        String body = "{ \"payer_id\": \"" + payerId + "\" }";
        HttpEntity<String> entity = new HttpEntity<>(body, headers);
        response = restTemplate.exchange(activateUrl, HttpMethod.POST, entity, String.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            return "approved";
        } else {
            return "failed";
        }
    }

    /**
     * Parsen der Antwort von PayPal, um den Abonnementstatus zu extrahieren
     * @param responseBody Antwort von PayPal
     * @return Abonnementstatus
     */
    private String parseSubscriptionStatus(String responseBody) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(responseBody);
            return rootNode.path("status").asText();
        } catch (Exception e) {
            log.atTrace().log(e.getMessage());
            return "Error parsing subscription status";
        }
    }

    /**
     * Kündigt ein Abonnement sowohl lokal, als auch bei PayPal
     * @param user Subscription User
     * @param subscriptionId Benötigte Id der Subscription
     * @return Wurde ein Abbonement gefunden
     */
    public boolean cancelSubscription(UserEntity user, String subscriptionId) {
        if(user.getLatestSubscription().getStatus().equals("CANCELLED")) {
            return true;
        }

        if (user.getLatestSubscription() != null) {
            SubscriptionEntity subscription = user.getLatestSubscription();

            boolean isPaypalCancelled = cancelPaypalSubscription(subscription.getSubscriptionId());

            if (!isPaypalCancelled) {
                return false;
            }

            subscription.setStatus("CANCELLED");
            subscriptionRepository.save(subscription);
            return true;
        }

        return false;
    }

    /**
     * Cancelled eine Subscription bei PayPal
     * @param paypalSubscriptionId Benötigte Id
     * @return Wurde gekündigt oder nicht
     */
    private boolean cancelPaypalSubscription(String paypalSubscriptionId) {
        try {
            String accessToken = getAccessToken();

            String url = PAYPAL_API_URL + "/" + paypalSubscriptionId + "/cancel";
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            String body = "{ \"reason\": \"User requested cancellation.\" }";
            HttpEntity<String> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            return response.getStatusCode() == HttpStatus.NO_CONTENT;
        } catch (Exception e) {
            log.atTrace().log(e.getMessage());
            return false;
        }
    }


    /**
     * pausiert die Subscription eines Users
     * @param user Der User der Subscription
     * @return wurde die Subscription pausiert oder nicht
     */
    public boolean pauseSubscription(UserEntity user) {
        if(user.getLatestSubscription().getStatus().equals("CANCELLED")) {
            return true;
        }

        if (user.getLatestSubscription() != null) {
            SubscriptionEntity subscription = user.getLatestSubscription();

            boolean isPaypalCancelled = pausePayPalSubscription(subscription.getSubscriptionId());

            if (!isPaypalCancelled) {
                return false;
            }

            subscription.setStatus("SUSPENDED");
            subscriptionRepository.save(subscription);
            return true;
        }

        return false;
    }


    /**
     * pausiert eine Subscription.
     * @param subscriptionId benötigte Id der Subscription
     * @return wurde die Subscription pausiert oder nicht
     */
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

            ResponseEntity<String> response = restTemplate.exchange(
                    PAYPAL_API_URL + "/" + subscriptionId + "/suspend",
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.atTrace().log(e.getMessage());
            return false;
        }
    }


    /**
     * Setzt eine pausierte Subscription fort
     * @param user der user der Subscription
     * @param subscriptionId die benötigte Id der Subscription
     * @return wurde die Subscription pausiert oder nicht
     */
    public boolean resumeSubscription(UserEntity user, String subscriptionId) {
        if(user.getLatestSubscription().getStatus().equals("ACTIVE")) {
            return true;
        }

        if (user.getLatestSubscription() != null) {
            SubscriptionEntity subscription = user.getLatestSubscription();

            boolean isPaypalResumed = resumePayPalSubscription(subscriptionId);

            if (!isPaypalResumed) {
                return false;
            }

            subscription.setStatus("ACTIVE");
            subscriptionRepository.save(subscription);
            return true;
        }

        return false;
    }


    /**
     * Setzt eine Subscription fort
     * @param subscriptionId benötigte Id für die Subscription
     * @return wurde die subscription fortgesetzt oder nicht
     */
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

            ResponseEntity<String> response = restTemplate.exchange(
                    PAYPAL_API_URL + "/" + subscriptionId + "/activate",
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.atTrace().log(e.getMessage());
            return false;
        }
    }


    /**
     * Holt sich die Zahlungsinformationen einer Subscrition für einen gewissen Zeitraum
     * @param subscriptionId benötigte Id der Subscription
     * @return Liste der transactions von PayPal
     */
    public List<TransactionSubscriptionEntity> getTransactionsForSubscription(String subscriptionId) {
        try {
            String accessToken = getAccessToken();

            String startTime = "2023-12-01T00:00:00Z";
            String endTime = "2025-12-27T23:59:59Z";

            String url = UriComponentsBuilder.fromHttpUrl(PAYPAL_API_URL + "/" + subscriptionId + "/transactions")
                    .queryParam("start_time", startTime)
                    .queryParam("end_time", endTime)
                    .toUriString();

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                String responseBody = response.getBody();
                return parseTransactions(responseBody);
            } else {
                return null;
            }
        } catch (Exception e) {
            log.atTrace().log(e.getMessage());
            return null;
        }
    }


    /**
     * Holt die genaueren Informationen aus der JSON Antwort von der payPal API
     * @param jsonResponse JSON Antwort von der PayPal API
     * @return Die Liste der geparsten Informationen oder null
     */
    private List<TransactionSubscriptionEntity> parseTransactions(String jsonResponse) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        try {
            JsonNode jsonNode = objectMapper.readTree(jsonResponse);
            JsonNode transactionsNode = jsonNode.path("transactions");

            return objectMapper.readerForListOf(TransactionSubscriptionEntity.class)
                    .readValue(transactionsNode);
        } catch (Exception e) {
            log.atTrace().log(e.getMessage());
            return new ArrayList<>();
        }
    }


    /**
     * Filtert die Liste der Transaktionen nach gewissen Paramtern
     * @param transactions Die Transaktionen
     * @param username Der Username
     * @param datefrom Das Startdatum
     * @param dateto Das Enddatum
     * @param amount Die minimale balance
     * @return gefilterte Liste der Transaktionen
     */
    public List<TransactionSubscriptionEntity> filterTransactions(
            List<TransactionSubscriptionEntity> transactions,
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
     * Filtert eine Liste von Zahlungen nach gewissen Paramtern
     * @param payments Die Liste der Zahlungen
     * @param username Der Username
     * @param reason Der Grund
     * @param datefrom Das Startdatum
     * @param dateto Das Enddatum
     * @param amount Die Minimale Balance
     * @return Gefilterte Liste von Zahlungen
     */
    public List<PaymentEntity> filterPayments(List<PaymentEntity> payments,
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