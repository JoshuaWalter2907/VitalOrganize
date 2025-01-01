package com.springboot.vitalorganize.service;

import com.springboot.vitalorganize.model.*;
import com.springboot.vitalorganize.model.UserEntity;
import com.springboot.vitalorganize.service.repositoryhelper.SubscriptionRepositoryService;
import com.springboot.vitalorganize.service.repositoryhelper.UserRepositoryService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@AllArgsConstructor
public class SubscriptionService {

    private final PaypalService paypalService;
    private final UserRepositoryService userRepositoryService;
    private final SubscriptionRepositoryService subscriptionRepositoryService;


    public String createSubscriptionRedirect(UserEntity userEntity) {
        // Überprüfen, ob der Benutzer bereits eine aktive Subscription hat
        if (userEntity.getLatestSubscription() != null && userEntity.getLatestSubscription().getStatus().equals("ACTIVE")) {
            return "/profile";  // Direkt weiterleiten, wenn die Subscription aktiv ist
        }

        // Überprüfen, ob die Subscription noch nicht abgelaufen ist
        if (userEntity.getLatestSubscription() != null && userEntity.getLatestSubscription().getEndTime().isAfter(LocalDateTime.now())) {
            return null;  // Keine neue Subscription erstellen, da noch gültig
        }

        // Wenn keine gültige Subscription vorhanden ist, eine neue erstellen
        String planId = "P-1DP07006BV376124WM5XEB6Q";  // Beispiel Plan ID
        return paypalService.createSubscription(planId, userEntity);  // PayPal-Service zur Erstellung der Subscription
    }

    public boolean cancelSubscription(UserEntity userEntity) {
        String subscriptionId = userEntity.getLatestSubscription().getSubscriptionId();
        return paypalService.cancelSubscription(userEntity, subscriptionId);
    }

    public boolean pauseSubscription(UserEntity userEntity) {
        // Geschäftslogik für das Pausieren der Subscription
        return paypalService.pauseSubscription(userEntity);
    }

    public boolean resumeSubscription(UserEntity userEntity) {
        // Geschäftslogik zum Fortsetzen der Subscription
        String subscriptionId = userEntity.getLatestSubscription().getSubscriptionId();
        return paypalService.resumeSubscription(userEntity, subscriptionId);
    }

    public boolean confirmSubscription(String subscriptionId, String token, UserEntity userEntity) {
        // Logik zur Bestätigung der Subscription über PayPal
        String payerId = paypalService.getPayerIdFromSubscription(subscriptionId);
        String approvalResponse = paypalService.confirmSubscription(subscriptionId, payerId);

        if ("approved".equals(approvalResponse)) {
            // Benutzerrolle ändern
            userEntity.setRole("MEMBER");
            userRepositoryService.saveUser(userEntity);

            // Subscription in der Datenbank speichern
            SubscriptionEntity subscriptionEntity = new SubscriptionEntity();
            subscriptionEntity.setSubscriptionId(subscriptionId);
            subscriptionEntity.setPayerId(payerId);
            subscriptionEntity.setPlanId("P-1DP07006BV376124WM5XEB6Q");  // Beispiel Plan ID
            subscriptionEntity.setStatus("ACTIVE");  // Setze den Status auf "ACTIVE"
            subscriptionEntity.setStartTime(String.valueOf(LocalDateTime.now()));  // Setze Startzeit
            subscriptionEntity.setNextBillingTime(String.valueOf(LocalDateTime.now().plusMonths(1).withHour(1).withMinute(0).withSecond(0).withNano(0)));  // Nächste Abrechnung
            subscriptionEntity.setEndTime(LocalDateTime.now().plusMonths(1).withHour(0).withMinute(1).withSecond(0).withNano(0));  // Endzeit
            subscriptionEntity.setUser(userEntity);  // Verknüpfung mit dem Benutzer

            // Subscription in der DB speichern
            subscriptionRepositoryService.saveSubscription(subscriptionEntity);

            return true;  // Erfolg
        }

        return false;  // Fehler bei der Bestätigung
    }

}
