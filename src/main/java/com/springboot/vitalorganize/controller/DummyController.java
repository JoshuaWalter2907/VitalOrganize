package com.springboot.vitalorganize.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.vitalorganize.model.*;
import com.springboot.vitalorganize.service.ChatService;
import com.springboot.vitalorganize.service.DirectChatRepository;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.Thread.sleep;

@Controller
@AllArgsConstructor
public class DummyController {

    private final DummyService dummyService;
    private final SimpMessagingTemplate brokerMessagingTemplate;
    private final DirectChatRepository directChatRepository;

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
        System.out.println(user);

        String name = user.getAttribute("name"); // Allgemeiner Benutzername
        String email = user.getAttribute("email"); // Allgemeine E-Mail
        int id = 0;
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

        UserEntity userEntity = userRepository.findUserEntityById((long) id);

        String photoUrl = userEntity.getProfilePictureUrl();

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
            Model model
    ) {
         // "google", "discord", "github"
        String email = user.getAttribute("email"); // Allgemeine E-Mail
        int id = 0;


        String themeCss = "/css/" + theme + "-theme.css";
        model.addAttribute("themeCss", themeCss);
        model.addAttribute("lang", lang);

        String provider = authentication.getAuthorizedClientRegistrationId();
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
        System.out.println("chatgroups: " + chatGroups);
        model.addAttribute("chatGroups", chatGroups);

        List<DirectChat> directChats = chatService.getDirectChats((long) id);  // Direkt-Chats
        model.addAttribute("directChats", directChats);

        List<MessageEntity> messages = null;
        UserEntity selectedUser = null;
        ChatGroup selectedGroup = null;
        DirectChat selectedDirectChat = null;
        String otherUserName = null;
        String otherUserPicture= null;

        if (groupId != null) {
            selectedGroup = chatGroupRepository.findById(groupId).orElse(null);
            messages = chatService.getGroupMessages(groupId, 0, 50);
        } else if (user2 != null) {
            selectedUser = userRepository.findUserEntityById(user2);
            otherUserName = selectedUser.getUsername();
            otherUserPicture = selectedUser.getProfilePictureUrl();
            selectedDirectChat = chatService.getDirectChat((long) id, user2);  // Hole DirectChat
            messages = chatService.getMessages((long) id, user2, 0, 50);
        }

        List<UserEntity> chatParticipants = chatService.getChatParticipants((long) id);
        System.out.println(chatParticipants);

        System.out.println(messages);

        List<Object> chatList = new ArrayList<>();
        chatList.addAll(directChats);
        chatList.addAll(chatGroups);

        System.out.println("die Chatlist ist: " + chatList);

        model.addAttribute("messages", messages);
        model.addAttribute("currentUser", username); // Der aktuelle Benutzer
        model.addAttribute("selectedUser", selectedUser);
        model.addAttribute("selectedGroup", selectedGroup);
        model.addAttribute("selectedDirectChat", selectedDirectChat);
        model.addAttribute("chatList", chatList);
        model.addAttribute("RecipientId", user2);
        model.addAttribute("SenderId", id);
        model.addAttribute("GroupId", groupId);
        model.addAttribute("otherUsername", otherUserName);
        model.addAttribute("otherUserPicture", otherUserPicture);

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
            System.out.println("Die gruppenId = " + "/topic/messages/group/" + groupId);
            System.out.println("message" + savedMessage);
            brokerMessagingTemplate.convertAndSend("/topic/messages/group/" + groupId, savedMessage);
        } else {
            int receiver = Integer.parseInt(RecipientId);
            savedMessage = chatService.sendMessage(sender, receiver, content);
            System.out.println("HIer war ich auch: " + receiver + " " + content);
            System.out.println("message" + savedMessage);
            brokerMessagingTemplate.convertAndSend("/topic/messages/" + receiver, savedMessage);
        }

        return savedMessage;
    }

    @GetMapping("/public-users")
    public String showPublicUsers(
            @RequestParam(value = "theme", defaultValue = "light") String theme,
            @RequestParam(value = "lang", defaultValue = "en") String lang,
            Model model
    ){
        String themeCss = "/css/" + theme + "-theme.css";
        model.addAttribute("themeCss", themeCss);
        model.addAttribute("lang", lang);

        List<UserEntity> publicUsers = userRepository.findAllByisPublic(true);
        model.addAttribute("publicUsers", publicUsers);
        return "public-users";
    }

    @PostMapping("/create-group")
    public String createGroup(
            @RequestParam List<Long> selectedUsers,
            @RequestParam("chatname") String chatName,
            @AuthenticationPrincipal OAuth2User user,
            OAuth2AuthenticationToken authentication,
            Principal principal
    ){
        int id=0;
        String email = user.getAttribute("email"); // Allgemeine E-Mail


        String provider = authentication.getAuthorizedClientRegistrationId();
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

        UserEntity currentUser = userRepository.findUserEntityById((long) id);


    if(selectedUsers.size() != 1){
        System.out.println(chatName + " " + selectedUsers);
        ChatGroup chatGroup = new ChatGroup();
        chatGroup.setName(chatName);
        List<UserEntity> publicUserSet = userRepository.findAllById(selectedUsers);
        System.out.println(publicUserSet);
        chatGroup.setUsers(publicUserSet);
        chatGroupRepository.save(chatGroup);
    }else{
        DirectChat directChat = new DirectChat();
        UserEntity otherUser = userRepository.findUserEntityById(selectedUsers.get(0));

        directChat.setUser1(currentUser);
        directChat.setUser2(otherUser);

        directChatRepository.save(directChat);
    }
        return "redirect:/chat";
    }


}
