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

@Slf4j
@Service
@AllArgsConstructor
public class PaypalService {

    private final SenderService senderService;
    private final PaymentRepositoryService paymentRepositoryService;
    private final UserRepositoryService userRepositoryService;
    private final FundRepositoryService fundRepositoryService;
    private final SubscriptionRepositoryService subscriptionRepositoryService;

    public final APIContext apiContext;
    private final PayPalConfig payPalConfig;
    private static final String PAYPAL_API_URL = "https://api-m.sandbox.paypal.com/v1/billing/subscriptions";



    public String createPaypalPayment(
            double amount,
            String type,
            String description,
            String email,
            Long fundid,
            Long userId
    ) throws PayPalRESTException {
        String currency = "EUR";
        // PayPal-Zahlung erstellen
        String cancelUrl = "http://localhost:8080/fund/payinto/cancel";
        String successUrl = "http://localhost:8080/fund/payinto/success";

        // Aufruf der Methode zur Erstellung der Zahlung
        return handlePaymentCreation(amount, currency, description, cancelUrl, successUrl);
    }


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

    public com.paypal.api.payments.Payment executePayment(
            String paymentId,
            String payerId
    ) throws PayPalRESTException {

        com.paypal.api.payments.Payment payment = new com.paypal.api.payments.Payment();
        payment.setId(paymentId);

        PaymentExecution paymentExecution = new PaymentExecution();
        paymentExecution.setPayerId(payerId);

        return payment.execute(apiContext, paymentExecution);
    }

    public void addPayment(Payment payment, Long id) {
        // Letzte Buchung abrufen
        Optional<Payment> latestTransaction = paymentRepositoryService.findLatestTransactionByFundId(id);
        double lastBalance = latestTransaction.map(Payment::getBalance).orElse(0.0);

        // Neuen Kontostand berechnen
        if (payment.getType() == PaymentType.EINZAHLEN) {
            payment.setBalance(lastBalance + payment.getAmount());
        } else if (payment.getType() == PaymentType.AUSZAHLEN) {
            payment.setBalance(lastBalance - payment.getAmount());
        }

        // Neue Buchung speichern
        paymentRepositoryService.savePayment(payment);
    }


    public double getCurrentBalance() {
        return paymentRepositoryService.findLatestTransaction().getBalance();
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

            assert body != null;
            return body.get("access_token");
        } else {
            throw new RuntimeException("Failed to fetch access token: " + response.getStatusCode());
        }
    }

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


        String accessToken = getAccessToken();  // Hol dir das AccessToken für die API

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
        UserEntity user = userRepositoryService.findByEmailAndProvider(email, provider);
        if (user.getProvider().equals("github"))
            email = user.getSendtoEmail();

        FundEntity fund = fundRepositoryService.findFundById(fundId);

        Payment zahlung = new Payment();
        zahlung.setDate(LocalDateTime.now());
        zahlung.setReason(description);
        zahlung.setAmount(Double.valueOf(amount));
        zahlung.setCurrency(currency);
        zahlung.setUser(user);
        zahlung.setFund(fund);


        if ("EINZAHLEN".equalsIgnoreCase(type)) {
            zahlung.setType(PaymentType.EINZAHLEN);
            com.paypal.api.payments.Payment payment = executePayment(paymentId, payerId);
            if ("approved".equals(payment.getState())) {
                addPayment(zahlung, id);
                senderService.sendConfirmationEmail(email, amount, currency, "paypal", description, type);
                return;
            }
        } else if ("AUSZAHLEN".equalsIgnoreCase(type)) {
            double balance = getCurrentBalance();
            if (Double.parseDouble(amount) > balance) {
                return;
            } else {
                zahlung.setType(PaymentType.AUSZAHLEN);
                executePayout(receiverEmail, amount, currency);
                addPayment(zahlung, id);
                senderService.sendConfirmationEmail(email, amount, currency, "paypal", description, type);
                return;
            }
        }
        throw new RuntimeException("Invalid payment type");
    }

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
            ObjectMapper objectMapper = new ObjectMapper();

            JsonNode rootNode = objectMapper.readTree(responseBody);

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
            log.atTrace().log(e.getMessage());
        }
        return null;  // Falls keine Approval-URL gefunden wurde
    }

    public String getPayerIdFromSubscription(String subscriptionId) {
        try {
            String accessToken = getAccessToken();  // Hole den Access Token

            String subscriptionUrl = "https://api-m.sandbox.paypal.com/v1/billing/subscriptions/" + subscriptionId;

            HttpURLConnection httpConn = (HttpURLConnection) new URL(subscriptionUrl).openConnection();
            httpConn.setRequestMethod("GET");
            httpConn.setRequestProperty("Authorization", "Bearer " + accessToken);
            httpConn.setRequestProperty("Content-Type", "application/json");

            InputStream responseStream = httpConn.getResponseCode() / 100 == 2
                    ? httpConn.getInputStream()
                    : httpConn.getErrorStream();

            Scanner s = new Scanner(responseStream).useDelimiter("\\A");
            String response = s.hasNext() ? s.next() : "";


            JsonObject jsonObject = JsonParser.parseString(response).getAsJsonObject();
            String payerId = jsonObject.getAsJsonObject("subscriber").get("payer_id").getAsString();

            return Objects.requireNonNullElse(payerId, "PayerID konnte nicht abgerufen werden.");  // Rückgabe der PayerID
        } catch (Exception e) {
            log.atTrace().log(e.getMessage());
            return "Fehler bei der Abfrage der PayerID";
        }
    }

    public String confirmSubscription(String subscriptionId, String payerId) {
        String accessToken = getAccessToken();  // OAuth Token holen

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        String getSubscriptionUrl = "https://api-m.sandbox.paypal.com/v1/billing/subscriptions/" + subscriptionId;

        ResponseEntity<String> response = restTemplate.exchange(getSubscriptionUrl, HttpMethod.GET, new HttpEntity<>(headers), String.class);

        String subscriptionStatus = parseSubscriptionStatus(response.getBody()); // Hier musst du den Status aus der Antwort extrahieren

        if (!"SUSPENDED".equalsIgnoreCase(subscriptionStatus)) {
            String suspendUrl = "https://api-m.sandbox.paypal.com/v1/billing/subscriptions/" + subscriptionId + "/suspend";
            response = restTemplate.exchange(suspendUrl, HttpMethod.POST, new HttpEntity<>(headers), String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                return "Error suspending subscription.";
            }
        }

        String activateUrl = "https://api-m.sandbox.paypal.com/v1/billing/subscriptions/" + subscriptionId + "/activate";
        String body = "{ \"payer_id\": \"" + payerId + "\" }"; // Falls PayerId erforderlich ist
        HttpEntity<String> entity = new HttpEntity<>(body, headers);
        response = restTemplate.exchange(activateUrl, HttpMethod.POST, entity, String.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            return "approved";
        } else {
            return "failed";
        }
    }

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

            // 2. Lokalen Status auf "CANCELLED" setzen
            subscription.setStatus("CANCELLED");
            subscriptionRepositoryService.saveSubscription(subscription);
            return true;
        }

        return false; // Subscription wurde nicht gefunden
    }

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

            return response.getStatusCode() == HttpStatus.NO_CONTENT; // NO_CONTENT (204) bedeutet Erfolg

        } catch (Exception e) {
            log.atTrace().log(e.getMessage());
            return false; // Fehler bei der Kündigung
        }
    }

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
            subscriptionRepositoryService.saveSubscription(subscription);
            return true;
        }

        return false;
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

            ResponseEntity<String> response = restTemplate.exchange(
                    PAYPAL_API_URL + "/" + subscriptionId + "/suspend",
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            return response.getStatusCode().is2xxSuccessful();

        } catch (Exception e) {
            log.atTrace().log(e.getMessage());
            return false; //
        }


    }

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
            subscriptionRepositoryService.saveSubscription(subscription);
            return true;
        }

        return false;
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

            ResponseEntity<String> response = restTemplate.exchange(
                    PAYPAL_API_URL + "/" + subscriptionId + "/activate",
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.atTrace().log(e.getMessage());;
            return false;
        }
    }

    public List<TransactionSubscription> getTransactionsForSubscription(String subscriptionId) {

        try {
            String accessToken = getAccessToken();

            String startTime = "2023-12-01T00:00:00Z";
            String endTime = "2025-12-27T23:59:59Z";

            // Erstellen des Endpunkt-URLs
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

    private List<TransactionSubscription> parseTransactions(String jsonResponse) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule()); // Registriert das Modul für Java 8 Zeittypen

        try {
            JsonNode jsonNode = objectMapper.readTree(jsonResponse);
            JsonNode transactionsNode = jsonNode.path("transactions");

            return objectMapper.readerForListOf(TransactionSubscription.class)
                    .readValue(transactionsNode);
        } catch (Exception e) {
            log.atTrace().log(e.getMessage());
            return new ArrayList<>();
        }
    }

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


    public List<Payment> filterPayments(List<Payment> payments,
                                        String username,
                                        String reason,
                                        LocalDate datefrom,
                                        LocalDate dateto,
                                        Long amount
    ) {

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