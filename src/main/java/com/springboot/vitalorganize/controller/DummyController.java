package com.springboot.vitalorganize.controller;

import com.springboot.vitalorganize.model.Dummy;
import com.springboot.vitalorganize.service.DummyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.util.Optional;
import java.util.stream.Stream;

@Controller
public class DummyController {

    private final DummyService dummyService;

    @Autowired
    public DummyController(DummyService dummyService) {
        this.dummyService = dummyService;
    }


    @RequestMapping("/")
    public String home(
            @RequestParam(value = "theme", defaultValue = "light") String theme,
            @RequestParam(value = "lang", defaultValue = "en") String lang,
            Model model) {
        // Dynamisch den Pfad zum CSS-Theme auswählen
        String themeCss = "/css/" + theme + "-theme.css";
        model.addAttribute("themeCss", themeCss);

        // Sprache dem Modell hinzufügen
        model.addAttribute("lang", lang);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if(auth != null && auth.isAuthenticated()) {
            model.addAttribute("username", auth.getName());
        }

        return "home"; // Name des Thymeleaf-Templates, z.B. "home.html"
    }

    @RequestMapping("/user")
    public OAuth2User user(@AuthenticationPrincipal OAuth2User user) {
        return user;
    }

    @GetMapping("/login")
    public String login(@RequestParam(value = "theme", defaultValue = "light") String theme,
                        @RequestParam(value = "lang", defaultValue = "en") String lang,
                        Model model) {

        // Dynamisch den Pfad zum CSS-Theme auswählen
        String themeCss = "/css/" + theme + "-theme.css";
        model.addAttribute("themeCss", themeCss);

        // Sprache dem Modell hinzufügen
        model.addAttribute("lang", lang);
        return "LoginPage";
    }

    // POST-Mapping für die Verarbeitung der Suchanfrage
    @PostMapping("/search")
    public String searchDummyById(@RequestParam("id") int id, Model model) {
        try {
            Dummy dummy = dummyService.getDummyById(id);
            model.addAttribute("dummy", dummy);
            return "testresponse";  // Zeigt das 'testresponse.html' Template an
        } catch (Exception e) {
            // Dummy nicht gefunden: Fehlernachricht hinzufügen
            model.addAttribute("errorMessage", "Kein Eintrag mit ID " + id + " gefunden.");
            return "test";  // Zeigt wieder 'test.html' mit Fehlermeldung an
        }
    }

    @GetMapping("/profile")
    public String profile(@RequestParam(value = "theme", defaultValue = "light") String theme,
                          @RequestParam(value = "lang", defaultValue = "en") String lang,
                          @AuthenticationPrincipal OAuth2User user, Model model) {

        System.out.println("Benutzerattribute: " + user.getAttributes());


        // Extrahiere den Authentifizierungs-Provider (Google oder Discord)
        String provider = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("");  // "google" oder "discord" sollte hier sein

        String name = user.getAttribute("name");  // Name des Benutzers (bei Google)
        String email = user.getAttribute("email");  // Email des Benutzers (bei beiden)
        String photoUrl = "";

        Object issuerObj = user.getAttribute("iss");
        String issuer = (issuerObj != null) ? issuerObj.toString() : null;

        System.out.println("issuer: " + issuer);

        if ("https://accounts.google.com".equals(issuer)) {
            provider = "google";
        } else {
            provider = "discord";
        }

        if ("google".equals(provider)) {
            // Google-spezifische Logik
            photoUrl = user.getAttribute("picture");  // Google Foto URL
        } else if ("discord".equals(provider)) {
            // Discord-spezifische Logik
            String discordId = user.getAttribute("id");  // Discord ID
            String avatarHash = user.getAttribute("avatar");  // Avatar-Hash von Discord

            if (discordId != null && avatarHash != null) {
                photoUrl = "https://cdn.discordapp.com/avatars/" + discordId + "/" + avatarHash + ".png";
            } else {
                // Standard-Avatar, falls kein Avatar vorhanden ist
                photoUrl = "https://cdn.discordapp.com/embed/avatars/0.png";
            }
        }
        // Füge die extrahierten Werte dem Model hinzu
        model.addAttribute("name", name);
        model.addAttribute("email", email);
        model.addAttribute("photo", photoUrl);  // Profilbild-URL

        // Dynamisch den Pfad zum CSS-Theme auswählen
        String themeCss = "/css/" + theme + "-theme.css";
        model.addAttribute("themeCss", themeCss);

        // Sprache dem Modell hinzufügen
        model.addAttribute("lang", lang);

        // Gib das Thymeleaf-Template zurück, das die Daten anzeigt
        return "user-profile";  // Der Name des Thymeleaf-Templates (z.B. profile.html)
    }
}
