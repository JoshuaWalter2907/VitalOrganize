package com.springboot.vitalorganize.controller;
import com.springboot.vitalorganize.model.Fund_Payment.SubscriptionRequestDTO;
import com.springboot.vitalorganize.service.SubscriptionService;
import com.springboot.vitalorganize.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;


/**
 * Der PaypalController verwaltet alle Endpunkte, die sich auf PayPal-API-Operationen beziehen.
 */
@Controller
@AllArgsConstructor
public class PaypalController {

    private UserService userService;
    private final SubscriptionService subscriptionService;


    /**
     * Endpoint um bei einer erfolgreiche Subscription weiterzuleiten
     * @param subscriptionRequestDTO Informationen bei einer Subscription
     * @return Weiterleitung auf die entsprechende Seite
     */
    @GetMapping("/subscription-success")
    public String handleSubscriptionSuccess(
            SubscriptionRequestDTO subscriptionRequestDTO) {
        try {
            boolean success = subscriptionService.confirmSubscription(subscriptionRequestDTO);

            if (success) {
                return "redirect:/profile";
            } else {
                return "redirect:/";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/";
        }
    }

    /**
     * Verarbeitet die Abbruchaktion einer Subscription.
     *
     * @return Weiterleitung zur Startseite
     */
    @GetMapping("/subscription-cancel")
    public String handleSubscriptionCancel() {
        return "redirect:/";
    }

    /**
     * Erstellt ein neues Abonnement und leitet den Benutzer zur PayPal-Seite weiter
     * @return Weiterleitung zur PayPal-Seite oder zur Startseite im Fehlerfall
     */
    @PostMapping("/create-subscription")
    public String createSubscription() {
        try {

            String redirectUri = subscriptionService.createSubscriptionRedirect();

            if (redirectUri != null) {
                return "redirect:" + redirectUri;
            } else {
                return "redirect:/";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/";
        }
    }

    /**
     * Storniert ein bestehendes Abonnement
     * @param request die aktuelle HTTP-Anfrage
     * @return Weiterleitung zur vorherigen Seite oder zur Profilseite bei Erfolg, sonst zur Startseite
     */
    @PostMapping("/cancel-subscription")
    public String cancelSubscription(
            HttpServletRequest request
            ) {

        String refererUrl = request.getHeader("Referer");


        try {
            boolean success = subscriptionService.cancelSubscription();

            if (success) {
                return "redirect:" + (refererUrl != null ? refererUrl : "/profile");
            } else {
                return "redirect:/";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/";
        }
    }

    /**
     * Pausiert ein bestehendes Abonnement.
     * @param request die aktuelle HTTP-Anfrage
     * @return Weiterleitung zur vorherigen Seite oder zur Profilseite bei Erfolg, sonst zur Startseite
     */
    @PostMapping("/pause-subscription")
    public String pauseSubscription(
            HttpServletRequest request) {
        String refererUrl = request.getHeader("Referer");

        try {

            boolean success = subscriptionService.pauseSubscription();

            if (success) {
                return "redirect:" + (refererUrl != null ? refererUrl : "/profile");
            } else {
                return "redirect:/";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/";
        }
    }

    /**
     * Setzt ein pausiertes Abonnement fort
     * @param request die aktuelle HTTP-Anfrage
     * @return Weiterleitung zur vorherigen Seite oder zur Profilseite bei Erfolg, sonst zur Startseite
     */
    @PostMapping("/resume-subscription")
    public String resumeSubscription(
            HttpServletRequest request) {
        String refererUrl = request.getHeader("Referer");

        try {
            boolean success = subscriptionService.resumeSubscription();

            if (success) {
                return "redirect:" + (refererUrl != null ? refererUrl : "/profile");
            } else {
                return "redirect:/";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/";
        }
    }


}