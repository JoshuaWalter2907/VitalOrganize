package com.springboot.vitalorganize.service;

import com.springboot.vitalorganize.model.*;
import com.springboot.vitalorganize.model.UserEntity;
import com.springboot.vitalorganize.service.repositoryhelper.SubscriptionRepositoryService;
import com.springboot.vitalorganize.service.repositoryhelper.UserRepositoryService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
/**
 * Service-Klasse zur Verwaltung von Abonnements (Subscriptions).
 * Diese Klasse bietet Methoden zur Erstellung, Bestätigung, Stornierung, Pausierung und Fortsetzung von Abonnements.
 */
@Service
@AllArgsConstructor
public class SubscriptionService {

    // Abhängigkeiten: PayPal-Service, UserRepositoryService und SubscriptionRepositoryService
    private final PaypalService paypalService;
    private final UserRepositoryService userRepositoryService;
    private final SubscriptionRepositoryService subscriptionRepositoryService;

    /**
     * Überprüft den Status des Abonnements des Benutzers und leitet ihn bei Bedarf zur PayPal-Zahlung um.
     *
     * @param userEntity Der Benutzer, dessen Abonnement überprüft werden soll
     * @return Eine Weiterleitungs-URL, entweder zur Profilseite oder zur PayPal-Subscription-Seite
     */
    public String createSubscriptionRedirect(UserEntity userEntity) {
        // Überprüfen, ob der Benutzer bereits eine aktive Subscription hat
        if (userEntity.getLatestSubscription() != null && userEntity.getLatestSubscription().getStatus().equals("ACTIVE")) {
            return "/profile";  // Wenn das Abonnement aktiv ist, Weiterleitung zur Profilseite
        }

        // Überprüfen, ob die Subscription noch nicht abgelaufen ist
        if (userEntity.getLatestSubscription() != null && userEntity.getLatestSubscription().getEndTime().isAfter(LocalDateTime.now())) {
            return null;  // Keine neue Subscription erstellen, da die aktuelle noch gültig ist
        }

        // Wenn keine gültige Subscription vorhanden ist, wird eine neue Subscription über PayPal erstellt
        String planId = "P-1DP07006BV376124WM5XEB6Q";  // Beispiel Plan ID
        return paypalService.createSubscription(planId, userEntity);  // Weiterleitung zur PayPal-Subscription-Seite
    }

    /**
     * Storniert das Abonnement des Benutzers.
     *
     * @param userEntity Der Benutzer, dessen Abonnement storniert werden soll
     * @return true, wenn das Abonnement erfolgreich storniert wurde, sonst false
     */
    public boolean cancelSubscription(UserEntity userEntity) {
        String subscriptionId = userEntity.getLatestSubscription().getSubscriptionId();
        return paypalService.cancelSubscription(userEntity, subscriptionId);  // Abonnement über PayPal stornieren
    }

    /**
     * Pausiert das Abonnement des Benutzers.
     *
     * @param userEntity Der Benutzer, dessen Abonnement pausiert werden soll
     * @return true, wenn das Abonnement erfolgreich pausiert wurde, sonst false
     */
    public boolean pauseSubscription(UserEntity userEntity) {
        // Logik für das Pausieren der Subscription
        return paypalService.pauseSubscription(userEntity);  // Abonnement über PayPal pausieren
    }

    /**
     * Setzt das Abonnement des Benutzers fort.
     *
     * @param userEntity Der Benutzer, dessen Abonnement fortgesetzt werden soll
     * @return true, wenn das Abonnement erfolgreich fortgesetzt wurde, sonst false
     */
    public boolean resumeSubscription(UserEntity userEntity) {
        String subscriptionId = userEntity.getLatestSubscription().getSubscriptionId();
        // Logik zum Fortsetzen der Subscription
        return paypalService.resumeSubscription(userEntity, subscriptionId);  // Abonnement über PayPal fortsetzen
    }

    /**
     * Bestätigt das Abonnement nach erfolgreicher PayPal-Zahlung.
     *
     * @param subscriptionId Die ID des Abonnements
     * @param token Der Bestätigungstoken von PayPal
     * @param userEntity Der Benutzer, dessen Abonnement bestätigt wird
     * @return true, wenn das Abonnement erfolgreich bestätigt wurde, sonst false
     */
    public boolean confirmSubscription(String subscriptionId, String token, UserEntity userEntity) {
        // Payer ID aus dem Abonnement von PayPal abrufen
        String payerId = paypalService.getPayerIdFromSubscription(subscriptionId);
        String approvalResponse = paypalService.confirmSubscription(subscriptionId, payerId);

        // Überprüfen, ob das Abonnement von PayPal genehmigt wurde
        if ("approved".equals(approvalResponse)) {
            // Benutzerrolle auf "MEMBER" ändern
            userEntity.setRole("MEMBER");
            userRepositoryService.saveUser(userEntity);  // Benutzer in der Datenbank speichern

            // Erstelle und speichere das Subscription-Objekt in der Datenbank
            SubscriptionEntity subscriptionEntity = new SubscriptionEntity();
            subscriptionEntity.setSubscriptionId(subscriptionId);
            subscriptionEntity.setPayerId(payerId);
            subscriptionEntity.setPlanId("P-1DP07006BV376124WM5XEB6Q");  // Beispiel Plan ID
            subscriptionEntity.setStatus("ACTIVE");  // Setze den Status auf "ACTIVE"
            subscriptionEntity.setStartTime(String.valueOf(LocalDateTime.now()));  // Startzeit des Abonnements
            subscriptionEntity.setNextBillingTime(String.valueOf(LocalDateTime.now().plusMonths(1).withHour(1).withMinute(0).withSecond(0).withNano(0)));  // Nächste Abrechnung
            subscriptionEntity.setEndTime(LocalDateTime.now().plusMonths(1).withHour(0).withMinute(1).withSecond(0).withNano(0));  // Endzeit des Abonnements
            subscriptionEntity.setUser(userEntity);  // Verknüpfung mit dem Benutzer

            subscriptionRepositoryService.saveSubscription(subscriptionEntity);  // Subscription in der DB speichern

            return true;  // Erfolgreiche Bestätigung des Abonnements
        }

        return false;  // Fehler bei der Bestätigung des Abonnements
    }

}
