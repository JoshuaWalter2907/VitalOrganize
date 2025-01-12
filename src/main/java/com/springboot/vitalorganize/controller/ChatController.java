package com.springboot.vitalorganize.controller;
import com.springboot.vitalorganize.model.*;
import com.springboot.vitalorganize.entity.*;
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
    public String showChatPage(ChatRequestDTO chatRequestDTO, Model model) {

        ChatResponseDTO responseDTO = chatService.getChatData(
                chatRequestDTO.getUser2(),
                chatRequestDTO.getGroup(),
                chatRequestDTO.getQuery()
        );

        model.addAttribute("chatData", responseDTO);

        return "chat/chat";
    }





    @GetMapping("/newChat")
    public String newChatPage(
            HttpServletRequest request,
            Model model
    ) {

        NewChatResponseDTO newChatResponseDTO = chatService.newChat(request.getRequestURI());
        model.addAttribute("newChat", newChatResponseDTO);

        return "chat/newChat";
    }


    @PostMapping("/create-group")
    public String createGroup(
            CreateGroupRequest createGroupRequest,
            Model model
    ) {
        UserEntity currentUser = userService.getCurrentUser();

        //Validiert die selektierten User, ob eine direkt Chat, ein Gruppenchat oder
        boolean isValid = chatService.validateCreateGroupRequest(createGroupRequest, model, currentUser);
        // Wenn eingabe nicht valide ist, wird zurückgeleitet
        if (!isValid) {
            return "redirect:/newChat";
        }

        // Wenn Eingabe valid ist, wird einen neue Gruppe oder ein neuer Direkt Chat erstellt
        chatService.createChat(createGroupRequest.getSelectedUsers(), createGroupRequest.getChatName(), currentUser);

        return "redirect:/chat";
    }

    @PostMapping("/chat/deleteChat")
    public String deleteChat(
            @RequestParam("chat-id") Long chatId,
            Model model
    ) {
        UserEntity currentUser = userService.getCurrentUser();

        //Schaut ob der Chat löschbar ist
        boolean isDeleted = chatService.deleteChatById(chatId, currentUser);
        if (!isDeleted) {
            //Wenn der Chat nicht gelöscht werden konnte
            model.addAttribute("errorMessage", "Chat konnte nicht gelöscht werden.");
            return "redirect:/chat";
        }

        return "redirect:/chat";
    }

    //Empfangen der Nachricht: Wie bereits per Email besprochen, ist das senden der Email mittels Server Sockets umgesetzt um das Frontend besser zu gestalten
    @MessageMapping("/chat/send")
    public void sendMessage(@Payload MessageDTO messageDTO) {
        try {
            chatService.handleMessageSending(messageDTO);
        } catch (IllegalArgumentException e) {
            System.err.println("Fehler beim Senden der Nachricht: " + e.getMessage());
        }
    }



}
