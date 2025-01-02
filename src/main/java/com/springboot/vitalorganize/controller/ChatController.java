package com.springboot.vitalorganize.controller;

import com.springboot.vitalorganize.dto.ChatDetail;
import com.springboot.vitalorganize.dto.ChatDetailsDTO;
import com.springboot.vitalorganize.dto.CreateGroupRequest;
import com.springboot.vitalorganize.dto.MessageDTO;
import com.springboot.vitalorganize.model.*;
import com.springboot.vitalorganize.service.ChatService;
import com.springboot.vitalorganize.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@Controller
@AllArgsConstructor
public class ChatController {

    private final UserService userService;
    private final ChatService chatService;


    @GetMapping("/chat")
    public String chat(
            @RequestParam(value = "user2", required = false) Long user2,
            @RequestParam(value = "group", required = false) Long groupId,
            @RequestParam(value = "query", required = false) String query,
            @AuthenticationPrincipal OAuth2User user,
            OAuth2AuthenticationToken authentication,
            Model model) {

        // Hole den aktuellen Benutzer
        UserEntity currentUser = userService.getCurrentUser(user, authentication);
        Long senderId = currentUser.getId();
        String username = currentUser.getUsername();

        model.addAttribute("currentUser", username);
        model.addAttribute("SenderId", senderId);

        // Hole alle relevanten Chat-Daten über den Service
        List<ChatGroup> chatGroups = chatService.getChatGroups(senderId);
        List<DirectChat> directChats = chatService.getDirectChats(senderId);
        List<UserEntity> chatParticipants = chatService.getChatParticipants(senderId);

        // Hole die gefilterte Liste von Chats, wenn ein Suchbegriff vorliegt
        List<Object> filteredChatList = chatService.filterChats(senderId, query);

        // Hole die Chat-Details für eine bestimmte Gruppe oder Benutzer
        ChatDetailsDTO chatDetailsDTO = chatService.prepareChatDetails(senderId, groupId, user2, query);

        // Bereite die Chat-Details vor
        List<ChatDetail> chatDetailsList = chatService.prepareChatDetailsList(filteredChatList);

        // Füge alle Daten zum Model hinzu
        model.addAttribute("chatGroups", chatGroups);
        model.addAttribute("directChats", directChats);
        model.addAttribute("chatParticipants", chatParticipants);
        model.addAttribute("messages", chatDetailsDTO.getMessages());
        model.addAttribute("selectedUser", chatDetailsDTO.getSelectedUser());
        model.addAttribute("selectedGroup", chatDetailsDTO.getSelectedGroup());
        model.addAttribute("selectedDirectChat", chatDetailsDTO.getSelectedDirectChat());
        model.addAttribute("chatDetails", chatDetailsList);
        model.addAttribute("otherUsername", chatDetailsDTO.getOtherUserName());
        model.addAttribute("otherUserPicture", chatDetailsDTO.getOtherUserPicture());
        model.addAttribute("GroupId", chatDetailsDTO.getGroupId());
        model.addAttribute("chatId", chatDetailsDTO.getChatId());
        model.addAttribute("RecipientId", chatDetailsDTO.getRecipientId());

        return "chat/chat"; // Zurück zur Chat-Seite
    }

    @GetMapping("/public-users")
    public String showPublicUsers(
            @AuthenticationPrincipal OAuth2User user,
            OAuth2AuthenticationToken authentication,
            HttpServletRequest request,
            Model model
    ) {
        UserEntity currentUser = userService.getCurrentUser(user, authentication);
        List<UserEntity> publicUsers = chatService.preparePublicUsersPage(currentUser.getId());

        Map<Character, List<UserEntity>> groupedUsers = chatService.groupUsersByInitial(publicUsers);

        model.addAttribute("groupedUsers", groupedUsers);
        model.addAttribute("currentUrl", request.getRequestURI());

        return "chat/newChat";
    }

    @PostMapping("/create-group")
    public String createGroup(
            @ModelAttribute CreateGroupRequest createGroupRequest,
            @AuthenticationPrincipal OAuth2User user,
            OAuth2AuthenticationToken authentication,
            Model model
    ) {
        UserEntity currentUser = userService.getCurrentUser(user, authentication);

        boolean isValid = chatService.validateCreateGroupRequest(createGroupRequest, model, currentUser);
        if (!isValid) {
            return "redirect:/public-users";
        }

        chatService.createChat(createGroupRequest.getSelectedUsers(), createGroupRequest.getChatName(), currentUser);

        return "redirect:/chat";
    }

    @PostMapping("/chat/deleteChat")
    public String deleteChat(
            @RequestParam("chat-id") Long chatId,
            @AuthenticationPrincipal OAuth2User user,
            OAuth2AuthenticationToken authentication,
            Model model
    ) {
        UserEntity currentUser = userService.getCurrentUser(user, authentication);

        boolean isDeleted = chatService.deleteChatById(chatId, currentUser);
        if (!isDeleted) {
            model.addAttribute("errorMessage", "Chat konnte nicht gelöscht werden.");
            return "chat";
        }

        return "redirect:/chat";
    }

    @MessageMapping("/chat/send")
    public void sendMessage(@Payload MessageDTO messageDTO) {
        try {
            chatService.handleMessageSending(messageDTO);
        } catch (IllegalArgumentException e) {
            // Optional: Fehlerbehandlung oder Logging
            System.err.println("Fehler beim Senden der Nachricht: " + e.getMessage());
        }
    }


}
