package com.springboot.vitalorganize.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.vitalorganize.model.*;
import com.springboot.vitalorganize.service.ChatService;
import com.springboot.vitalorganize.service.DummyService;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.codehaus.groovy.antlr.treewalker.SourcePrinter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.lang.Thread.sleep;

@Controller
@AllArgsConstructor
public class DummyController {

    private final DummyService dummyService;
    private final MessageRepository messageRepository;
    private final SimpMessagingTemplate brokerMessagingTemplate;

    private UserRepository userRepository;
    private ChatGroupRepository chatGroupRepository;

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
            @RequestParam(value = "user2", required = false) Long user2,
            @RequestParam(value = "group", required = false) Long groupId,
            @AuthenticationPrincipal OAuth2User user,
            OAuth2AuthenticationToken authentication,
            HttpSession session,
            Model model
    ) {


        String provider = authentication.getAuthorizedClientRegistrationId(); // "google", "discord", "github"
        String email = user.getAttribute("email"); // Allgemeine E-Mail
        int id = 0;


        String themeCss = "/css/" + theme + "-theme.css";
        model.addAttribute("themeCss", themeCss);
        model.addAttribute("lang", lang);

        switch (provider) {
            case "google":
                id = Math.toIntExact(userRepository.findByEmailAndProvider(email, provider).getId());
                break;
            case "discord":
                id = Math.toIntExact(userRepository.findByEmailAndProvider(email, provider).getId());
                break;
            case "github":
                String username = user.getAttribute("login");
                System.out.println(username);
                email = username + "@github.com";
                id = Math.toIntExact(userRepository.findByEmailAndProvider(email, provider).getId());
                break;
        }


        System.out.println(provider + " " + email + " " + id);

        UserEntity existingUser = userRepository.findUserEntityById((long) id);
        System.out.println(existingUser);
        String username = existingUser.getUsername();
        System.out.println(username);

        List<ChatGroup> chatGroups = chatService.getChatGroups((long) id);
        model.addAttribute("chatGroups", chatGroups);

        List<MessageEntity> messages = null;
        UserEntity selectedUser = null;
        ChatGroup selectedGroup = null;

        if (groupId != null) {
            selectedGroup = chatGroupRepository.findById(groupId).orElse(null);
            messages = chatService.getGroupMessages(groupId, 0, 50);
        } else if (user2 != null) {
            selectedUser = userRepository.findUserEntityById(user2);
            messages = chatService.getMessages((long) id, user2, 0, 50);
        }

        List<UserEntity> chatParticipants = chatService.getChatParticipants((long) id);
        System.out.println(chatParticipants);

        System.out.println(messages);

        model.addAttribute("messages", messages);
        model.addAttribute("currentUser", username); // Der aktuelle Benutzer
        model.addAttribute("selectedUser", selectedUser);
        model.addAttribute("selectedGroup", selectedGroup);
        model.addAttribute("chatParticipants", chatParticipants);
        model.addAttribute("RecipientId", user2);
        model.addAttribute("SenderId", id);

        return "chat";
    }

    @MessageMapping("/chat/send")
    public MessageEntity sendMessage(@Payload String messageEntity) throws JsonProcessingException {
        System.out.println("Ich war hier");
        // Wenn du den Benutzer mit der Nachricht validieren möchtest, kannst du dies hier tun.
        // Zum Beispiel: Die MessageEntity enthält möglicherweise bereits den Sender und Empfänger.

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> messageMap = objectMapper.readValue(messageEntity, Map.class);

        System.out.println(messageEntity);

        String content = (String) messageMap.get("content");
        System.out.println(content);
        String SenderId = (String) messageMap.get("senderId");
        System.out.println(SenderId);
        String RecipientId = (String) messageMap.get("recipientId");
        System.out.println(RecipientId);
        String chatGroupId = (String) messageMap.get("chatGroupId");

        int sender = Integer.parseInt(SenderId);
        MessageEntity savedMessage;

        if (chatGroupId != null) {
            Long groupId = Long.parseLong(chatGroupId);
            savedMessage = chatService.sendGroupMessage((long) sender, groupId, content);
            brokerMessagingTemplate.convertAndSend("/topic/messages/group/" + groupId, savedMessage);
        } else {
            int receiver = Integer.parseInt(RecipientId);
            savedMessage = chatService.sendMessage(sender, receiver, content);
            brokerMessagingTemplate.convertAndSend("/topic/messages/" + receiver, savedMessage);
        }

        return savedMessage;
    }

}
