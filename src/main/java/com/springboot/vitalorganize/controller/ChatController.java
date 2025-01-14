package com.springboot.vitalorganize.controller;
import com.springboot.vitalorganize.model.Chat.*;
import com.springboot.vitalorganize.service.ChatService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@AllArgsConstructor
public class ChatController {

    private final ChatService chatService;


    /**
     * Der Hauptendpoint für alles was mit chats zu tun hat
     * @param chatRequestDTO Alle benötigten Informationen aus dem Frontend um die Seite auch im Nachhinein korrekt zu laden
     * @param model Model zum hinzufügen von Attributen im Frontend
     * @return Rückgabe des HTML-files für die Seite Chat
     */
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

    /**
     * Endpoint zum erstellen eines neuen Chats
     * @param request Request um die aktuelle URL zu erhalten
     * @param model Model zum hinzufügen von Attributen im Frontend
     * @return Rückgabe des HTML-files für die Seite Chat
     */
    @GetMapping("/newChat")
    public String newChatPage(
            HttpServletRequest request,
            Model model
    ) {

        NewChatResponseDTO newChatResponseDTO = chatService.newChat(request.getRequestURI());
        model.addAttribute("newChat", newChatResponseDTO);

        return "chat/newChat";
    }


    /**
     * Conroller nimmt die Anfrage eine neue Gruppe oder einen neuen Direkt Chat zu erstellen an, validiert diese und leitet entsprechend weiter
     * @param createGroupRequest Alle Informationen die benötigt werden um eine neue Gruppe oder einen neuen Direkt Chat zu erstellen
     * @param model Model zum hinzufügen von Attributen im Frontend
     * @return Rückgabe des HTML-files für die Seite Chat
     */
    @PostMapping("/create-group")
    public String createGroup(
            CreateChatGroupRequestDTO createGroupRequest,
            Model model
    ) {

        boolean isValid = chatService.validateCreateGroupRequest(createGroupRequest, model);
        if (!isValid) {
            return "redirect:/newChat";
        }

        chatService.createChat(
                createGroupRequest.getSelectedUsers(),
                createGroupRequest.getChatName());

        return "redirect:/chat";
    }

    /**
     * Endpoint um einen Chat zu löschen, falls diese löschbar ist
     * @param deleteChatRequestDTO Informationen die benötigt werden um einen Chat zu löschen
     * @param model Model zum hinzufügen von Attributen im Frontend
     * @return Rückgabe des HTML-files für die Seite Chat
     */
    @PostMapping("/chat/deleteChat")
    public String deleteChat(
            DeleteChatRequestDTO deleteChatRequestDTO,
            Model model
    ) {

        boolean isDeletable = chatService.deleteChatById(
                deleteChatRequestDTO.getChatId()
                );

        if (!isDeletable) {
            model.addAttribute("errorMessage", "Chat konnte nicht gelöscht werden.");
            return "redirect:/chat";
        }

        return "redirect:/chat";
    }

    /**
     * Endpoint zum empfangen von Nachrichten und verteilen dieser via MessageBroker und Websockets (wie per email besprochen)
     * @param messageDTO Informationen einer Nachricht
     */
    @MessageMapping("/chat/send")
    public void sendMessage(@Payload MessageDTO messageDTO) {
        try {
            chatService.handleMessageSending(messageDTO);
        } catch (IllegalArgumentException e) {
            System.err.println("Fehler beim Senden der Nachricht: " + e.getMessage());
        }
    }



}
