package com.springboot.vitalorganize.controller;

import com.paypal.api.payments.Links;
import com.paypal.api.payments.Payment;
import com.paypal.base.rest.PayPalRESTException;
import com.springboot.vitalorganize.model.*;
import com.springboot.vitalorganize.service.PaypalService;
import com.springboot.vitalorganize.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

import java.time.LocalDateTime;

@Controller
@AllArgsConstructor
@Slf4j
public class PaypalController {

    private PaypalService paypalService;
    private UserService userService;

    private final UserRepository userRepository;

    private final subscriptionRepository subscriptionRepository;

    @PostMapping("/create-subscription")
    public String createSubscription(@AuthenticationPrincipal OAuth2User user,
                                     OAuth2AuthenticationToken authentication,
                                     RedirectAttributes redirectAttributes,
                                     HttpServletRequest request) {
        try {

            String uri = request.getHeader("Referer");

            UserEntity userEntity = userService.getCurrentUser(user, authentication);

            if (userEntity.getLatestSubscription() != null && userEntity.getLatestSubscription().getStatus().equals("ACTIVE")) {
                return "redirect:/profile";
            }

            if (userEntity.getLatestSubscription() != null && userEntity.getLatestSubscription().getEndTime().isAfter(LocalDateTime.now())) {
                return "redirect:" + uri;
            }
            // Der Plan ID ist ein Beispiel, es kann von der Anwendung abhängen
            String planId = "P-1DP07006BV376124WM5XEB6Q";  // Plan ID aus Ihrer PayPal Konfiguration

            // PayPal Subscription erstellen
            String approvalUrl = paypalService.createSubscription(planId, userEntity);

            if (approvalUrl != null) {
                // Benutzer zur PayPal-URL weiterleiten
                redirectAttributes.addFlashAttribute("message", "Subscription erfolgreich erstellt!");
                return "redirect:" + approvalUrl;  // Weiterleitung zur PayPal-Seite zur Bestätigung
            } else {
                redirectAttributes.addFlashAttribute("error", "Fehler bei der Erstellung der Subscription.");
                return "redirect:/";  // Falls ein Fehler auftritt, zur Fehlerseite weiterleiten
            }
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Fehler bei der Subscription.");
            return "redirect:/";  // Fehlerbehandlung
        }
    }

    @PostMapping("/cancel-subscription")
    public String cancelSubscription(
            @AuthenticationPrincipal OAuth2User user,
            OAuth2AuthenticationToken authentication,
            HttpServletRequest request
    ) {
        String refererUrl = request.getHeader("Referer");

        String uri = request.getRequestURI();
        UserEntity userEntity = userService.getCurrentUser(user, authentication);

        boolean success = paypalService.cancelSubscription(userEntity, userEntity.getLatestSubscription().getSubscriptionId());

        if (success) {
            return "redirect:" + refererUrl;
        }
        return "redirect:/";
    }

    @PostMapping("/pause-subscription")
    public String pauseSubscription(
            @AuthenticationPrincipal OAuth2User user,
            OAuth2AuthenticationToken authentication,
            HttpServletRequest request
    ) {
        String refererUrl = request.getHeader("Referer");

        UserEntity userEntity = userService.getCurrentUser(user, authentication);

        boolean success = paypalService.pauseSubscription(userEntity, userEntity.getLatestSubscription().getSubscriptionId());

        if (success) {
            return "redirect:" + refererUrl;
        }
        return "redirect:/";
    }

    @PostMapping("/resume-subscription")
    public String resumeSubscription(
            @AuthenticationPrincipal OAuth2User user,
            OAuth2AuthenticationToken authentication,
            HttpServletRequest request
    ) {
        String refererUrl = request.getHeader("Referer");

        UserEntity userEntity = userService.getCurrentUser(user, authentication);

        boolean success = paypalService.resumeSubscription(userEntity, userEntity.getLatestSubscription().getSubscriptionId());

        if (success) {
            return "redirect:" + refererUrl;
        }
        return "redirect:/";
    }


    @GetMapping("/subscription-success")
    public String handleSubscriptionSuccess(@RequestParam("subscription_id") String subscription_id,
                                            @RequestParam("token") String token,
                                            RedirectAttributes redirectAttributes,
                                            @AuthenticationPrincipal OAuth2User user,
                                            OAuth2AuthenticationToken authentication
    ) {
        try {

            UserEntity userEntity = userService.getCurrentUser(user, authentication);
            // Erstelle eine Anfrage, um die Subscription zu bestätigen

            String payerId = paypalService.getPayerIdFromSubscription(subscription_id);

            String approvalResponse = paypalService.confirmSubscription(subscription_id, payerId);
            System.out.println(approvalResponse);

            if ("approved".equals(approvalResponse)) {
                redirectAttributes.addFlashAttribute("message", "Subscription erfolgreich!");
                userEntity.setRole("MEMBER");
                userRepository.save(userEntity);

                SubscriptionEntity subscriptionEntity = new SubscriptionEntity();
                subscriptionEntity.setSubscriptionId(subscription_id);
                subscriptionEntity.setPayerId(payerId);
                subscriptionEntity.setPlanId("P-1DP07006BV376124WM5XEB6Q");  // Hier kannst du die planId dynamisch setzen
                subscriptionEntity.setStatus("ACTIVE");  // Setze den Status als "ACTIVE"
                subscriptionEntity.setStartTime(String.valueOf(LocalDateTime.now()));  // Setze das Startdatum
                subscriptionEntity.setNextBillingTime(String.valueOf(LocalDateTime.now().plusMonths(1).withHour(1).withMinute(0).withSecond(0).withNano(0)));  // Beispiel für das nächste Abrechnungsdatum
                subscriptionEntity.setEndTime(LocalDateTime.now().plusMonths(1).withHour(0).withMinute(1).withSecond(0).withNano(0));
                subscriptionEntity.setUser(userEntity);  // Verknüpfe die Subscription mit dem User
                // Speichere die Subscription in der Datenbank
                subscriptionRepository.save(subscriptionEntity);


                return "redirect:/profile";  // Weiterleitung auf die Erfolgseite
            } else {
                redirectAttributes.addFlashAttribute("error", "Fehler bei der Bestätigung der Subscription.");
                return "redirect:/";  // Falls die Bestätigung fehlschlägt
            }
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Fehler bei der Subscription.");
            return "redirect:/";  // Fehlerbehandlung
        }
    }

    @GetMapping("/subscription-cancel")
    public String handleSubscriptionCancel(){
        return "redirect:/";
    }
}