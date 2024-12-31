package com.springboot.vitalorganize.controller;
import com.springboot.vitalorganize.model.*;

import com.springboot.vitalorganize.repository.SubscriptionRepository;
import com.springboot.vitalorganize.repository.UserRepository;
import com.springboot.vitalorganize.service.PaypalService;
import com.springboot.vitalorganize.service.SubscriptionService;
import com.springboot.vitalorganize.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@AllArgsConstructor
public class PaypalController {

    private UserService userService;
    private final SubscriptionService subscriptionService;


    @PostMapping("/create-subscription")
    public String createSubscription(@AuthenticationPrincipal OAuth2User user,
                                     OAuth2AuthenticationToken authentication,
                                     RedirectAttributes redirectAttributes) {
        try {
            UserEntity userEntity = userService.getCurrentUser(user, authentication);

            // Die Geschäftslogik zur Überprüfung und Erstellung der Subscription wird in den Service ausgelagert
            String redirectUri = subscriptionService.createSubscriptionRedirect(userEntity);

            // Erfolgreiche Weiterleitung zur PayPal-Seite
            if (redirectUri != null) {
                redirectAttributes.addFlashAttribute("message", "Subscription erfolgreich erstellt!");
                return "redirect:" + redirectUri;
            } else {
                // Fehler bei der Erstellung der Subscription
                redirectAttributes.addFlashAttribute("error", "Fehler bei der Erstellung der Subscription.");
                return "redirect:/";
            }
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Fehler bei der Subscription.");
            return "redirect:/";  // Fehlerbehandlung
        }
    }

    @PostMapping("/cancel-subscription")
    public String cancelSubscription(@AuthenticationPrincipal OAuth2User user,
                                     OAuth2AuthenticationToken authentication,
                                     HttpServletRequest request,
                                     RedirectAttributes redirectAttributes) {
        String refererUrl = request.getHeader("Referer");

        UserEntity userEntity = userService.getCurrentUser(user, authentication);

        try {
            boolean success = subscriptionService.cancelSubscription(userEntity);

            if (success) {
                redirectAttributes.addFlashAttribute("message", "Subscription erfolgreich storniert!");
                return "redirect:" + (refererUrl != null ? refererUrl : "/profile");  // Sicherstellen, dass es eine gültige URL gibt
            } else {
                redirectAttributes.addFlashAttribute("error", "Fehler beim Stornieren der Subscription.");
                return "redirect:/";  // Fehlerfall
            }
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Fehler beim Stornieren der Subscription.");
            return "redirect:/";  // Fehlerbehandlung
        }
    }

    @PostMapping("/pause-subscription")
    public String pauseSubscription(@AuthenticationPrincipal OAuth2User user,
                                    OAuth2AuthenticationToken authentication,
                                    HttpServletRequest request,
                                    RedirectAttributes redirectAttributes) {
        String refererUrl = request.getHeader("Referer");

        try {
            UserEntity userEntity = userService.getCurrentUser(user, authentication);

            boolean success = subscriptionService.pauseSubscription(userEntity);

            if (success) {
                redirectAttributes.addFlashAttribute("message", "Subscription erfolgreich pausiert!");
                return "redirect:" + (refererUrl != null ? refererUrl : "/profile");  // Sicherstellen, dass es eine gültige URL gibt
            } else {
                redirectAttributes.addFlashAttribute("error", "Fehler beim Pausieren der Subscription.");
                return "redirect:/";  // Fehlerfall
            }
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Fehler beim Pausieren der Subscription.");
            return "redirect:/";  // Fehlerbehandlung
        }
    }

    @PostMapping("/resume-subscription")
    public String resumeSubscription(@AuthenticationPrincipal OAuth2User user,
                                     OAuth2AuthenticationToken authentication,
                                     HttpServletRequest request,
                                     RedirectAttributes redirectAttributes) {
        String refererUrl = request.getHeader("Referer");

        try {
            UserEntity userEntity = userService.getCurrentUser(user, authentication);

            boolean success = subscriptionService.resumeSubscription(userEntity);

            if (success) {
                redirectAttributes.addFlashAttribute("message", "Subscription erfolgreich fortgesetzt!");
                return "redirect:" + (refererUrl != null ? refererUrl : "/profile");  // Fallback auf /profile, wenn Referer nicht vorhanden
            } else {
                redirectAttributes.addFlashAttribute("error", "Fehler beim Fortsetzen der Subscription.");
                return "redirect:/";  // Fehlerfall
            }
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Fehler beim Fortsetzen der Subscription.");
            return "redirect:/";  // Fehlerbehandlung
        }
    }


    @GetMapping("/subscription-success")
    public String handleSubscriptionSuccess(@RequestParam("subscription_id") String subscriptionId,
                                            @RequestParam("token") String token,
                                            RedirectAttributes redirectAttributes,
                                            @AuthenticationPrincipal OAuth2User user,
                                            OAuth2AuthenticationToken authentication) {
        try {
            UserEntity userEntity = userService.getCurrentUser(user, authentication);
            boolean success = subscriptionService.confirmSubscription(subscriptionId, token, userEntity);

            if (success) {
                redirectAttributes.addFlashAttribute("message", "Subscription erfolgreich!");
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