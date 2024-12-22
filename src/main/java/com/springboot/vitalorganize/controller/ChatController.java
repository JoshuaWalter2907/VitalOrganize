package com.springboot.vitalorganize.controller;

import com.springboot.vitalorganize.dto.ChatDetail;
import com.springboot.vitalorganize.dto.MessageDTO;
import com.springboot.vitalorganize.model.*;
import com.springboot.vitalorganize.service.ChatService;
import com.springboot.vitalorganize.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Controller
@AllArgsConstructor
public class ChatController {

    private final UserService userService;
    private final SimpMessagingTemplate brokerMessagingTemplate;

    private UserRepository userRepository;
    private ChatGroupRepository chatGroupRepository;

    private final ChatService chatService;

    @GetMapping("/chat")
    public String chat(
            @RequestParam(value = "user2", required = false) Long user2,
            @RequestParam(value = "group", required = false) Long groupId,
            @RequestParam(value = "query", required = false) String query,
            @AuthenticationPrincipal OAuth2User user,
            OAuth2AuthenticationToken authentication,
            Model model) {

        UserEntity currentUser = userService.getCurrentUser(user, authentication);
        Long senderId = currentUser.getId();
        String username = currentUser.getUsername();
        System.out.println(username);

        // Dynamische Attribute setzen
        model.addAttribute("currentUser", username);
        model.addAttribute("SenderId", senderId);

        // Chats und Teilnehmer abrufen
        List<ChatGroup> chatGroups = chatService.getChatGroups(senderId);
        List<DirectChat> directChats = chatService.getDirectChats(senderId);
        List<UserEntity> chatParticipants = chatService.getChatParticipants(senderId);
        List<Object> filteredChatList = new ArrayList<>();


        // Suchlogik, um nach Benutzern zu suchen, deren Namen den `query`-Parameter enthalten
        if (query != null && !query.trim().isEmpty()) {
            // Suche nach Benutzernamen, die den `query`-Text enthalten
            List<UserEntity> filteredUsers = userRepository.findByUsernameContainingIgnoreCase(query);

            // Nur die Benutzer, mit denen der aktuelle Benutzer bereits einen Chat hat, werden zurückgegeben
            List<UserEntity> usersWithChats = new ArrayList<>();
            for (UserEntity userEntity : filteredUsers) {
                // Überprüfen, ob der Benutzer eine Direktnachricht mit dem aktuellen Benutzer hat
                if (chatService.getDirectChat(senderId, userEntity.getId()) != null) {
                    usersWithChats.add(userEntity);
                }
            }

            // Optional: Suche nach Chats für die gefilterten Benutzer
            for (UserEntity filteredUser : usersWithChats) {
                // Suche nach Direktnachrichten mit jedem dieser Benutzer
                DirectChat directChat = chatService.getDirectChat(senderId, filteredUser.getId());
                if (directChat != null) {
                    filteredChatList.add(directChat);
                }
            }

            List<ChatGroup> filteredChatGroups = chatGroupRepository.findByNameContaining(query);

            for(ChatGroup chatGroup : filteredChatGroups) {
                if(chatService.getChatGroups(senderId).contains(chatGroup)) {
                    filteredChatList.add(chatGroup);
                }
            }


        } else {

        }

        // Gruppennachrichten abrufen
        List<MessageEntity> messages = null;
        UserEntity selectedUser = null;
        ChatGroup selectedGroup = null;
        DirectChat selectedDirectChat = null;
        String otherUserName = null;
        String otherUserPicture = null;

        if (groupId != null) {
            selectedGroup = chatGroupRepository.findById(groupId).orElse(null);
            messages = chatService.getGroupMessages(groupId, 0, 50);
            model.addAttribute("GroupId", groupId);
            model.addAttribute("chatId", groupId); // Zum Löschen der Gruppe
        } else if (user2 != null) {
            selectedUser = userService.getUserById(user2);
            selectedDirectChat = chatService.getDirectChat(senderId, user2);
            messages = chatService.getMessages(senderId, user2, 0, 50);
            if (selectedUser != null) {
                otherUserName = selectedUser.getUsername();
                otherUserPicture = selectedUser.getProfilePictureUrl();
            }
            model.addAttribute("RecipientId", user2);
            if (selectedDirectChat != null) {
                model.addAttribute("chatId", selectedDirectChat.getId()); // Zum Löschen des Direkt-Chats
            } else {
                model.addAttribute("chatId", null); // Falls kein Direktchat existiert
            }
        }

        System.out.println("direct chats: " + directChats);

        // Chats und Nachrichten ins Model hinzufügen
        if (filteredChatList.isEmpty()) {
            filteredChatList.addAll(directChats);
            filteredChatList.addAll(chatGroups);
        }
        System.out.println("filtered chats: " + filteredChatList);
        List<ChatDetail> chatDetails = new ArrayList<>();

        for (Object chat : filteredChatList) {
            String lastMessageContent = "No messages yet";
            LocalDateTime lastMessageTime = null;

            // Prüfen, ob es sich um einen DirectChat handelt
            if (chat instanceof DirectChat directChat) {
                MessageEntity lastMessage = chatService.getLastMessage(directChat.getId());
                System.out.println(lastMessage + " " + directChat.getId());
                if (lastMessage != null) {
                    lastMessageContent = lastMessage.getContent();
                    lastMessageTime = lastMessage.getTimestamp();
                }

                // ChatDetail-Objekt für DirectChat erstellen
                chatDetails.add(new ChatDetail(directChat, lastMessageContent, lastMessageTime));

                // Prüfen, ob es sich um eine ChatGroup handelt
            } else if (chat instanceof ChatGroup) {
                ChatGroup chatGroup = (ChatGroup) chat;
                MessageEntity lastMessage = chatService.getLastGroupMessage(chatGroup.getId());
                if (lastMessage != null) {
                    lastMessageContent = lastMessage.getContent();
                    lastMessageTime = lastMessage.getTimestamp();
                }

                // ChatDetail-Objekt für ChatGroup erstellen
                chatDetails.add(new ChatDetail(chatGroup, lastMessageContent, lastMessageTime));
            }
        }




        for (ChatDetail chatDetail : chatDetails) {
        }

        model.addAttribute("chatGroups", chatGroups);
        model.addAttribute("directChats", directChats);
        model.addAttribute("chatParticipants", chatParticipants);
        model.addAttribute("messages", messages);
        model.addAttribute("selectedUser", selectedUser);
        model.addAttribute("selectedGroup", selectedGroup);
        model.addAttribute("selectedDirectChat", selectedDirectChat);
        model.addAttribute("chatDetails", chatDetails);
        model.addAttribute("otherUsername", otherUserName);
        model.addAttribute("otherUserPicture", otherUserPicture);

        return "chat"; // Zurück zur Chat-Seite mit den gefilterten Ergebnissen
    }


    @MessageMapping("/chat/send")
    public void sendMessage (@Payload MessageDTO messageDTO){
        // Überprüfe, ob ein Inhalt vorhanden ist
        if (messageDTO.getContent() == null || messageDTO.getContent().isEmpty()) {
            // Rückgabe einer Fehlerantwort oder Ausnahme
            return;
        }

        MessageEntity savedMessage;

        // Prüfen, ob die Nachricht eine Gruppen- oder Direktnachricht ist
        if (messageDTO.getChatGroupId() != null) {
            UserEntity sender = userRepository.findById(messageDTO.getSenderId()).orElse(null);
            // Senden einer Gruppennachricht
            savedMessage = chatService.sendGroupMessage(messageDTO.getSenderId(), messageDTO.getChatGroupId(), messageDTO.getContent());
            savedMessage.setSender(sender);
            brokerMessagingTemplate.convertAndSend("/topic/messages/group/" + messageDTO.getChatGroupId(), savedMessage);
        } else if (messageDTO.getRecipientId() != null) {
            // Senden einer Direktnachricht
            savedMessage = chatService.sendMessage(messageDTO.getSenderId(), messageDTO.getRecipientId(), messageDTO.getContent());
            System.out.println("/topic/messages/" + messageDTO.getRecipientId());
            brokerMessagingTemplate.convertAndSend("/topic/messages/" + messageDTO.getRecipientId(), savedMessage);
        } else {
            // Fehlerbehandlung: keine Empfänger- oder Gruppen-ID
            throw new IllegalArgumentException("Weder Empfänger noch Gruppen-ID angegeben.");
        }

    }

    @GetMapping("/public-users")
    public String showPublicUsers (
            @RequestParam(value = "theme", defaultValue = "light") String theme,
            @RequestParam(value = "lang", defaultValue = "en") String lang,
            HttpServletRequest request,
            Model model
    ){
        String currentUrl = request.getRequestURI().toString();
        chatService.preparePublicUsersPage(model, theme, lang);

        model.addAttribute("currentUrl", currentUrl);

        return "newChat";
    }

    @PostMapping("/create-group")
    public String createGroup (
            @RequestParam("selectedUsers") List < Long > selectedUsers,
            @RequestParam("chatname") String chatName,
            @RequestParam(value = "theme", defaultValue = "light") String theme,
            @RequestParam(value = "lang", defaultValue = "en") String lang,
            @RequestParam(value = "currentUrl") String currentUrl,
            @AuthenticationPrincipal OAuth2User user,
            OAuth2AuthenticationToken authentication,
            Model model
    ){
        System.out.println(currentUrl);
        chatService.preparePublicUsersPage(model, theme, lang);

        if (selectedUsers.size() > 1 && (chatName == null || chatName.trim().isEmpty())) {
            model.addAttribute("errorMessage", "Bitte geben Sie einen Gruppennamen ein.");
            return "newChat"; // Der Name der Thymeleaf-Vorlage
        }

        // Holen des aktuellen Benutzers aus dem UserService
        UserEntity currentUser = userService.getCurrentUser(user, authentication);

        // Erstellen der Gruppe oder DirectChat über den ChatService
        chatService.createChat(selectedUsers, chatName, currentUser);

        // Weiterleitung zum Chat
        return "redirect:/chat";
    }

    @PostMapping("/chat/deleteChat")
    public String deleteChat (
            @RequestParam("chat-id") Long chatId,
            @AuthenticationPrincipal OAuth2User user,
            OAuth2AuthenticationToken authentication
    ){
        UserEntity currentUser = userService.getCurrentUser(user, authentication);

        // Finde den Chat, entweder eine Gruppe oder ein Direkt-Chat
        Object chat = chatService.getChatById(chatId);
        if (chat instanceof ChatGroup) {
            chatService.deleteGroupChat((ChatGroup) chat, currentUser);
        } else if (chat instanceof DirectChat) {
            chatService.deleteDirectChat((DirectChat) chat, currentUser);
        }

        return "redirect:/chat"; // Weiterleitung zur Chat-Liste oder einer anderen Seite
    }
}
