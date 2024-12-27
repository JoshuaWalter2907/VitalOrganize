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




    @GetMapping("/paypal")
    public String paypal(
            @RequestParam(value = "theme", defaultValue = "light") String theme,
            @RequestParam(value = "lang", defaultValue = "en") String lang,
            Model model
    ) {

        model.addAttribute("themeCss", userService.getThemeCss(theme));
        model.addAttribute("lang", lang);
        model.addAttribute("balance", paypalService.getCurrentBalance());


        return "paypal";
    }

    @PostMapping("/paypal/create")
    public RedirectView createPaypal(
            @RequestParam("method") String method,
            @RequestParam("amount") String amount,
            @RequestParam("currency") String currency,
            @RequestParam("description") String description,
            @RequestParam("type") String type,
            @RequestParam("email") String email,
            @AuthenticationPrincipal OAuth2User user,
            HttpSession session
    ) {
        // Daten in die Session speichern
        session.setAttribute("amount", amount);
        session.setAttribute("currency", currency);
        session.setAttribute("description", description);
        session.setAttribute("type", type);
        session.setAttribute("email", email);
        session.setAttribute("method", method);

        try {
            String cancelUrl = "http://localhost:8080/paypal/cancel";
            String successUrl = "http://localhost:8080/paypal/success";

            String approvalUrl = paypalService.handlePaymentCreation(
                    Double.parseDouble(amount),
                    currency,
                    method,
                    description,
                    cancelUrl,
                    successUrl
            );

            return new RedirectView(approvalUrl);

        } catch (PayPalRESTException e) {
            log.error("Error occurred while creating PayPal payment", e);
            return new RedirectView("/paypal/error");
        }
    }


    @GetMapping("/paypal/success")
    public RedirectView paypalSuccess(
            @RequestParam("paymentId") String paymentId,
            @RequestParam("PayerID") String payerId,
            @AuthenticationPrincipal OAuth2User user,
            OAuth2AuthenticationToken authentication,
            HttpSession session
    ) {
        // Daten aus der Session lesen
        String type = (String) session.getAttribute("type");
        String amount = (String) session.getAttribute("amount");
        String currency = (String) session.getAttribute("currency");
        String description = (String) session.getAttribute("description");
        String receiverEmail = (String) session.getAttribute("email");
        String method = (String) session.getAttribute("method");

        // Benutzerinformationen verarbeiten
        String provider = authentication.getAuthorizedClientRegistrationId();
        String email = paypalService.getEmailForUser(user, provider);

        // Zahlungsabwicklung und Ergebnis erhalten
        try {
            String redirectUrl = paypalService.processPayment(
                    paymentId, payerId, type, amount, currency, description,
                    receiverEmail, email, method, provider
            );
            return new RedirectView(redirectUrl);
        } catch (Exception e) {
            log.error("Error during PayPal success processing", e);
            return new RedirectView("/paypal/error");
        }
    }

    @PostMapping("/create-subscription")
    public String createSubscription(@AuthenticationPrincipal OAuth2User user,
                                     OAuth2AuthenticationToken authentication,
                                     RedirectAttributes redirectAttributes) {
        try {

            UserEntity userEntity = userService.getCurrentUser(user,authentication);

            if(userEntity.getLatestSubscription() != null && userEntity.getLatestSubscription().getStatus().equals("ACTIVE")) {
                return "redirect:/profile";
            }
            // Der Plan ID ist ein Beispiel, es kann von der Anwendung abhängen
            String planId = "P-1DP07006BV376124WM5XEB6Q";  // Plan ID aus Ihrer PayPal Konfiguration

            // PayPal Subscription erstellen
            String approvalUrl = paypalService.createSubscription(planId, userEntity);

            if (approvalUrl != null) {
                // Benutzer in der Datenbank aktualisieren
                paypalService.updateUserSubscriptionStatus(userEntity, true, approvalUrl);

                // Benutzer zur PayPal-URL weiterleiten
                redirectAttributes.addFlashAttribute("message", "Subscription erfolgreich erstellt!");
                return "redirect:" + approvalUrl;  // Weiterleitung zur PayPal-Seite zur Bestätigung
            } else {
                redirectAttributes.addFlashAttribute("error", "Fehler bei der Erstellung der Subscription.");
                return "redirect:/error";  // Falls ein Fehler auftritt, zur Fehlerseite weiterleiten
            }
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Fehler bei der Subscription.");
            return "redirect:/error";  // Fehlerbehandlung
        }
    }

    @PostMapping("/cancel-subscription")
    public String cancelSubscription(
            @AuthenticationPrincipal OAuth2User user,
            OAuth2AuthenticationToken authentication,
            HttpServletRequest request
    ){
        UserEntity userEntity = userService.getCurrentUser(user,authentication);

        boolean success = paypalService.cancelSubscription(userEntity, userEntity.getLatestSubscription().getSubscriptionId());

        if(success) {
            return "redirect:/profile";
        }
        return "redirect:/error";
    }

    @PostMapping("/pause-subscription")
    public String pauseSubscription(
            @AuthenticationPrincipal OAuth2User user,
            OAuth2AuthenticationToken authentication,
            HttpServletRequest request
    ){
        UserEntity userEntity = userService.getCurrentUser(user,authentication);

        boolean success = paypalService.pauseSubscription(userEntity, userEntity.getLatestSubscription().getSubscriptionId());

        if(success) {
            return "redirect:/profile";
        }
        return "redirect:/error";
    }

    @PostMapping("/resume-subscription")
    public String resumeSubscription(
            @AuthenticationPrincipal OAuth2User user,
            OAuth2AuthenticationToken authentication
    ) {
        UserEntity userEntity = userService.getCurrentUser(user, authentication);

        boolean success = paypalService.resumeSubscription(userEntity, userEntity.getLatestSubscription().getSubscriptionId());

        if (success) {
            return "redirect:/profile";
        }
        return "redirect:/error";
    }




    @GetMapping("/subscription-success")
    public String handleSubscriptionSuccess(@RequestParam("subscription_id") String subscription_id,
                                            @RequestParam("token") String token,
                                            RedirectAttributes redirectAttributes,
                                            @AuthenticationPrincipal OAuth2User user,
                                            OAuth2AuthenticationToken authentication) {
        try {

            UserEntity userEntity = userService.getCurrentUser(user,authentication);
            // Erstelle eine Anfrage, um die Subscription zu bestätigen

            String payerId = paypalService.getPayerIdFromSubscription(subscription_id);

            String approvalResponse = paypalService.confirmSubscription(subscription_id, payerId);
            System.out.println(approvalResponse);

            if ("approved".equals(approvalResponse)) {
                redirectAttributes.addFlashAttribute("message", "Subscription erfolgreich!");
                System.out.println(userEntity.getRole());
                userEntity.setRole("MEMBER");
                System.out.println(userEntity.getRole());
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
                return "redirect:/error";  // Falls die Bestätigung fehlschlägt
            }
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Fehler bei der Subscription.");
            return "redirect:/error";  // Fehlerbehandlung
        }
    }






    @GetMapping("/paypal/cancel")
    public String paypalCancel(){
        return "paypal";
    }

    @GetMapping("/paypal/error")
    public String paypalError(){
        return "paymentError";
    }

}
