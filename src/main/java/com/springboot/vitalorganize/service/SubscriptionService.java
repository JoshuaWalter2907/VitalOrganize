package com.springboot.vitalorganize.service;

import com.springboot.vitalorganize.entity.*;
import com.springboot.vitalorganize.entity.Profile_User.UserEntity;
import com.springboot.vitalorganize.model.Fund_Payment.SubscriptionRequestDTO;
import com.springboot.vitalorganize.repository.SubscriptionRepository;
import com.springboot.vitalorganize.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
/**
 * Service-Klasse zur Verwaltung von Abonnements
 */
@Service
@AllArgsConstructor
public class SubscriptionService {

    private final PaypalService paypalService;
    private final UserService userService;
    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;

    /**
     * Überprüft den Status des Abonnements des Benutzers und leitet ihn bei Bedarf zur PayPal-Zahlung um.
     * @return Eine Weiterleitungs-URL, entweder zur Profilseite oder zur PayPal-Subscription-Seite
     */
    public String createSubscriptionRedirect() {
        UserEntity userEntity = userService.getCurrentUser();
        if (userEntity.getLatestSubscription() != null && userEntity.getLatestSubscription().getStatus().equals("ACTIVE")) {
            return "/profile";
        }

        if (userEntity.getLatestSubscription() != null && userEntity.getLatestSubscription().getEndTime().isAfter(LocalDateTime.now())) {
            return null;
        }

        String planId = "P-1DP07006BV376124WM5XEB6Q";
        return paypalService.createSubscription(planId, userEntity);
    }

    /**
     * Storniert das Abonnement des Benutzers.
     * @return true, wenn das Abonnement erfolgreich storniert wurde, sonst false
     */
    public boolean cancelSubscription() {
        UserEntity userEntity = userService.getCurrentUser();
        String subscriptionId = userEntity.getLatestSubscription().getSubscriptionId();
        return paypalService.cancelSubscription(userEntity, subscriptionId);
    }

    /**
     * Pausiert das Abonnement des Benutzers.
     * @return true, wenn das Abonnement erfolgreich pausiert wurde, sonst false
     */
    public boolean pauseSubscription() {
        UserEntity userEntity = userService.getCurrentUser();
        return paypalService.pauseSubscription(userEntity);
    }

    /**
     * Setzt das Abonnement des Benutzers fort.
     * @return true, wenn das Abonnement erfolgreich fortgesetzt wurde, sonst false
     */
    public boolean resumeSubscription() {
        UserEntity userEntity = userService.getCurrentUser();
        String subscriptionId = userEntity.getLatestSubscription().getSubscriptionId();
        return paypalService.resumeSubscription(userEntity, subscriptionId);
    }

    /**
     * Bestätigt das Abonnement nach erfolgreicher PayPal-Zahlung
     * @return true, wenn das Abonnement erfolgreich bestätigt wurde, sonst false
     */
    public boolean confirmSubscription(SubscriptionRequestDTO subscriptionRequestDTO) {
        UserEntity userEntity = userService.getCurrentUser();
        String payerId = paypalService.getPayerIdFromSubscription(subscriptionRequestDTO.getSubscription_id());
        String approvalResponse = paypalService.confirmSubscription(subscriptionRequestDTO.getSubscription_id(), payerId);


        if ("approved".equals(approvalResponse)) {

            userEntity.setRole("MEMBER");
            userRepository.save(userEntity);


            SubscriptionEntity subscriptionEntity = new SubscriptionEntity();
            subscriptionEntity.setSubscriptionId(subscriptionRequestDTO.getSubscription_id());
            subscriptionEntity.setPayerId(payerId);
            subscriptionEntity.setPlanId("P-1DP07006BV376124WM5XEB6Q");
            subscriptionEntity.setStatus("ACTIVE");
            subscriptionEntity.setStartTime(String.valueOf(LocalDateTime.now()));
            subscriptionEntity.setNextBillingTime(String.valueOf(LocalDateTime.now().plusMonths(1).withHour(1).withMinute(0).withSecond(0).withNano(0)));
            subscriptionEntity.setEndTime(LocalDateTime.now().plusMonths(1).withHour(0).withMinute(1).withSecond(0).withNano(0));
            subscriptionEntity.setUser(userEntity);

            subscriptionRepository.save(subscriptionEntity);

            return true;
        }

        return false;
    }

}
