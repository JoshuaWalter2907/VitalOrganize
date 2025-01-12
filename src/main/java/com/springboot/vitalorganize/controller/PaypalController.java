package com.springboot.vitalorganize.controller;
import com.springboot.vitalorganize.entity.*;
import com.springboot.vitalorganize.service.SubscriptionService;
import com.springboot.vitalorganize.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


/**
 * Der PaypalController verwaltet alle Endpunkte, die sich auf PayPal-Subscription-Operationen beziehen.
 *
 * Dazu gehören das Erstellen, Bestätigen, Pausieren, Fortsetzen und Stornieren von Abonnements.
 */
@Controller
@AllArgsConstructor
public class PaypalController {

    private UserService userService;
    private final SubscriptionService subscriptionService;


    /**
     * Verarbeitet eine erfolgreiche Bestätigung der Subscription.
     *
     * @param subscriptionId die ID des Abonnements
     * @param token der Authentifizierungstoken
     * @param redirectAttributes für Flash-Nachrichten während der Weiterleitung
     * @param user der aktuell authentifizierte Benutzer
     * @param authentication das OAuth2-Authentifizierungstoken
     * @return Weiterleitung zur Profilseite bei Erfolg, sonst zur Startseite
     */
    @GetMapping("/subscription-success")
    public String handleSubscriptionSuccess(
            @RequestParam("subscription_id") String subscriptionId,
            @RequestParam("token") String token,
            RedirectAttributes redirectAttributes,
            @AuthenticationPrincipal OAuth2User user,
            OAuth2AuthenticationToken authentication) {
        try {
            UserEntity userEntity = userService.getCurrentUser(user, authentication);
            boolean success = subscriptionService.confirmSubscription(subscriptionId, token, userEntity);

            if (success) {
                redirectAttributes.addFlashAttribute("message", "Subscription erfolgreich!");
                return "redirect:/profile";
            } else {
                redirectAttributes.addFlashAttribute("error", "Fehler bei der Bestätigung der Subscription.");
                return "redirect:/";
            }
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Fehler bei der Subscription.");
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
     * Erstellt ein neues Abonnement und leitet den Benutzer zur PayPal-Seite weiter.
     *
     * @param user der aktuell authentifizierte Benutzer
     * @param authentication das OAuth2-Authentifizierungstoken
     * @param redirectAttributes für Flash-Nachrichten während der Weiterleitung
     * @return Weiterleitung zur PayPal-Seite oder zur Startseite im Fehlerfall
     */
    @PostMapping("/create-subscription")
    public String createSubscription(
            @AuthenticationPrincipal OAuth2User user,
            OAuth2AuthenticationToken authentication,
            RedirectAttributes redirectAttributes) {
        try {
            UserEntity userEntity = userService.getCurrentUser(user, authentication);

            String redirectUri = subscriptionService.createSubscriptionRedirect(userEntity);

            if (redirectUri != null) {
                redirectAttributes.addFlashAttribute("message", "Subscription erfolgreich erstellt!");
                return "redirect:" + redirectUri;
            } else {
                redirectAttributes.addFlashAttribute("error", "Fehler bei der Erstellung der Subscription.");
                return "redirect:/";
            }
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Fehler bei der Subscription.");
            return "redirect:/";
        }
    }

    /**
     * Storniert ein bestehendes Abonnement.
     *
     * @param user der aktuell authentifizierte Benutzer
     * @param authentication das OAuth2-Authentifizierungstoken
     * @param request die aktuelle HTTP-Anfrage
     * @param redirectAttributes für Flash-Nachrichten während der Weiterleitung
     * @return Weiterleitung zur vorherigen Seite oder zur Profilseite bei Erfolg, sonst zur Startseite
     */
    @PostMapping("/cancel-subscription")
    public String cancelSubscription(
            @AuthenticationPrincipal OAuth2User user,
            OAuth2AuthenticationToken authentication,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        String refererUrl = request.getHeader("Referer");

        UserEntity userEntity = userService.getCurrentUser(user, authentication);

        try {
            boolean success = subscriptionService.cancelSubscription(userEntity);

            if (success) {
                redirectAttributes.addFlashAttribute("message", "Subscription erfolgreich storniert!");
                return "redirect:" + (refererUrl != null ? refererUrl : "/profile");
            } else {
                redirectAttributes.addFlashAttribute("error", "Fehler beim Stornieren der Subscription.");
                return "redirect:/";
            }
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Fehler beim Stornieren der Subscription.");
            return "redirect:/";
        }
    }

    /**
     * Pausiert ein bestehendes Abonnement.
     *
     * @param user der aktuell authentifizierte Benutzer
     * @param authentication das OAuth2-Authentifizierungstoken
     * @param request die aktuelle HTTP-Anfrage
     * @param redirectAttributes für Flash-Nachrichten während der Weiterleitung
     * @return Weiterleitung zur vorherigen Seite oder zur Profilseite bei Erfolg, sonst zur Startseite
     */
    @PostMapping("/pause-subscription")
    public String pauseSubscription(
            @AuthenticationPrincipal OAuth2User user,
            OAuth2AuthenticationToken authentication,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        String refererUrl = request.getHeader("Referer");

        try {
            UserEntity userEntity = userService.getCurrentUser(user, authentication);

            boolean success = subscriptionService.pauseSubscription(userEntity);

            if (success) {
                redirectAttributes.addFlashAttribute("message", "Subscription erfolgreich pausiert!");
                return "redirect:" + (refererUrl != null ? refererUrl : "/profile");
            } else {
                redirectAttributes.addFlashAttribute("error", "Fehler beim Pausieren der Subscription.");
                return "redirect:/";
            }
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Fehler beim Pausieren der Subscription.");
            return "redirect:/";
        }
    }

    /**
     * Setzt ein pausiertes Abonnement fort.
     *
     * @param user der aktuell authentifizierte Benutzer
     * @param authentication das OAuth2-Authentifizierungstoken
     * @param request die aktuelle HTTP-Anfrage
     * @param redirectAttributes für Flash-Nachrichten während der Weiterleitung
     * @return Weiterleitung zur vorherigen Seite oder zur Profilseite bei Erfolg, sonst zur Startseite
     */
    @PostMapping("/resume-subscription")
    public String resumeSubscription(
            @AuthenticationPrincipal OAuth2User user,
            OAuth2AuthenticationToken authentication,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        String refererUrl = request.getHeader("Referer");

        try {
            UserEntity userEntity = userService.getCurrentUser(user, authentication);

            boolean success = subscriptionService.resumeSubscription(userEntity);

            if (success) {
                redirectAttributes.addFlashAttribute("message", "Subscription erfolgreich fortgesetzt!");
                return "redirect:" + (refererUrl != null ? refererUrl : "/profile");
            } else {
                redirectAttributes.addFlashAttribute("error", "Fehler beim Fortsetzen der Subscription.");
                return "redirect:/";
            }
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Fehler beim Fortsetzen der Subscription.");
            return "redirect:/";
        }
    }


}