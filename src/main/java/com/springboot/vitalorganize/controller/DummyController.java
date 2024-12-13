package com.springboot.vitalorganize.controller;

import com.springboot.vitalorganize.model.MessageEntity;
import com.springboot.vitalorganize.model.UserRepository;
import com.springboot.vitalorganize.model.Dummy;
import com.springboot.vitalorganize.model.UserEntity;
import com.springboot.vitalorganize.service.ChatService;
import com.springboot.vitalorganize.service.DummyService;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static java.lang.Thread.sleep;

@Controller
@AllArgsConstructor
public class DummyController {

    private final DummyService dummyService;

    private UserRepository userRepository;

    private final ChatService chatService;

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
                          @AuthenticationPrincipal OAuth2User user,
                          OAuth2AuthenticationToken authentication, // Für `registrationId`
                          Model model) {

        System.out.println("Benutzerattribute: " + user.getAttributes());

        // Extrahiere den Authentifizierungs-Provider
        String provider = authentication.getAuthorizedClientRegistrationId(); // "google", "discord", "github"
        System.out.println(provider);

        String name = user.getAttribute("name"); // Allgemeiner Benutzername
        String email = user.getAttribute("email"); // Allgemeine E-Mail
        String photoUrl = "";

        // Provider-spezifische Logik
        switch (provider) {
            case "google":
                photoUrl = user.getAttribute("picture"); // Google Profilbild
                break;

            case "discord":
                String discordId = user.getAttribute("id"); // Discord ID
                String avatarHash = user.getAttribute("avatar"); // Discord Avatar-Hash

                if (discordId != null && avatarHash != null) {
                    photoUrl = "https://cdn.discordapp.com/avatars/" + discordId + "/" + avatarHash + ".png";
                } else {
                    photoUrl = "https://cdn.discordapp.com/embed/avatars/0.png"; // Standardavatar
                }
                break;

            case "github":
                name = user.getAttribute("login"); // GitHub Login-Name
                photoUrl = user.getAttribute("avatar_url"); // GitHub Profilbild
                break;

            default:
                throw new IllegalArgumentException("Unbekannter Provider: " + provider);
        }

        System.out.println(photoUrl);


        // Füge die extrahierten Werte dem Model hinzu
        model.addAttribute("name", name);
        model.addAttribute("email", email);
        model.addAttribute("photo", photoUrl); // Profilbild-URL

        // Dynamisch den Pfad zum CSS-Theme auswählen
        String themeCss = "/css/" + theme + "-theme.css";
        model.addAttribute("themeCss", themeCss);

        // Sprache dem Modell hinzufügen
        model.addAttribute("lang", lang);

        // Gib das Thymeleaf-Template zurück, das die Daten anzeigt
        return "user-profile"; // Der Name des Thymeleaf-Templates (z.B. profile.html)
    }

    @GetMapping("/profileaddition")
    public String profileAdditionGet(@RequestParam(value = "theme", defaultValue = "light") String theme,Model model,
                                     @AuthenticationPrincipal OAuth2User user,
                                     OAuth2AuthenticationToken authentication,
                                     HttpSession session) {

        String provider = authentication.getAuthorizedClientRegistrationId();
        String email = user.getAttribute("email");
        System.out.println(user);

        if (provider.equals("github")) {
            String username = user.getAttribute("login");
            System.out.println(username);
            email = username + "@github.com"; // Dummy-E-Mail erstellen
            System.out.println(email);
        }

        System.out.println(email + " " + provider);

        UserEntity existingUser = userRepository.findByEmailAndProvider(email, provider);

        model.addAttribute("user", existingUser);
        model.addAttribute("birthDate", existingUser.getBirthday());

        if(existingUser != null && !existingUser.getUsername().isEmpty()) {
            return "redirect:/profile";
        }else{
            return "Profile Additions";
        }

    }

    @PostMapping("/profileaddition")
    public String profileAddition(@RequestParam("inputString") String inputString,
                                  @RequestParam(name = "isPublic", required = false) boolean status,
                                  @RequestParam("birthDate") String birthDate,
                                  Model model,
                                  HttpSession session,
                                  @AuthenticationPrincipal OAuth2User user,
                                  OAuth2AuthenticationToken authentication) {

        String provider = authentication.getAuthorizedClientRegistrationId();
        String email = user.getAttribute("email");

        if (provider.equals("github")) {
            String username = user.getAttribute("login");
            System.out.println(username);
            email = username + "@github.com"; // Dummy-E-Mail erstellen
            System.out.println(email);
        }

        System.out.println(email + " " + provider);

        UserEntity existingUser = userRepository.findByEmailAndProvider(email, provider);
        System.out.println(existingUser);


        boolean usernameExists = userRepository.existsByUsername(inputString);
        System.out.println(usernameExists);
        System.out.println(status);

        existingUser.setPublic(status);
        existingUser.setBirthday(LocalDate.parse(birthDate));

        if (usernameExists) {
            return "redirect:/profileaddition"; // HTML-Seite mit Vorschlägen anzeigen
        }

        existingUser.setUsername(inputString);
        userRepository.save(existingUser);

        System.out.println(inputString);

        return "redirect:/profile";


    }

    @GetMapping("/chat")
    public String chat(
            @RequestParam(value = "theme", defaultValue = "light") String theme,
            @RequestParam(value = "lang", defaultValue = "en") String lang,
            @RequestParam(value = "user1", required = true) int user1,
            @RequestParam(value = "user2", required = true) int user2,
            @AuthenticationPrincipal OAuth2User user,
            OAuth2AuthenticationToken authentication,
            Model model
    ) {

        String username = "joshua";

        String themeCss = "/css/" + theme + "-theme.css";
        model.addAttribute("themeCss", themeCss);
        model.addAttribute("lang", lang);

        List<MessageEntity> messages = chatService.getMessages((long) user1, (long) user2, 0, 50);
        System.out.println(messages);

        model.addAttribute("messages", messages);
        model.addAttribute("currentUser", username); // Der aktuelle Benutzer

        return "chat";
    }

    @PostMapping("/send")
    public String sendMessage(
            @RequestParam String sender,
            @RequestParam String receiver,
            @RequestParam String content,
            @RequestParam(value = "theme", defaultValue = "light") String theme,
            @RequestParam(value = "lang", defaultValue = "en") String lang,
            RedirectAttributes redirectAttributes
    ) {
        // Nachricht speichern
        chatService.sendMessage(sender, receiver, content);

        // Parameter zurückgeben, um die Seite neu zu laden
        redirectAttributes.addAttribute("theme", theme);
        redirectAttributes.addAttribute("lang", lang);
        redirectAttributes.addAttribute("user1", sender);
        redirectAttributes.addAttribute("user2", receiver);
        return "redirect:/chat";
    }



}
