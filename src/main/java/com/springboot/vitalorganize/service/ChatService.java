package com.springboot.vitalorganize.service;

import com.springboot.vitalorganize.model.*;
import com.springboot.vitalorganize.entity.*;
import com.springboot.vitalorganize.service.repositoryhelper.ChatGroupRepositoryService;
import com.springboot.vitalorganize.service.repositoryhelper.DirektChatRepositoryService;
import com.springboot.vitalorganize.service.repositoryhelper.MessageRepositoryService;
import com.springboot.vitalorganize.service.repositoryhelper.UserRepositoryService;
import lombok.AllArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service-Klasse für die Verwaltung von Chat-Nachrichten, -Gruppen und -Teilnehmern.
 * Diese Klasse bietet Methoden zum Senden von Nachrichten, Erstellen von Chats und Abrufen von Chat-Details.
 */
@Service
@AllArgsConstructor
public class ChatService {

    private final UserRepositoryService userRepositoryService;
    private final MessageRepositoryService messageRepositoryService;
    private final DirektChatRepositoryService direktChatRepositoryService;
    private final ChatGroupRepositoryService chatGroupRepositoryService;
    private SimpMessagingTemplate brokerMessagingTemplate;
    private final UserService userService;

    /**
     * Gibt eine Liste von Nachrichten zwischen zwei Benutzern zurück.
     *
     * @param user1 der erste Benutzer
     * @param user2 der zweite Benutzer
     * @param page die Seitenzahl für die Paginierung
     * @param size die Seitengröße für die Paginierung
     * @return eine Liste von Nachrichten
     */
    public List<MessageEntity> getMessages(Long user1, Long user2, int page, int size) {
        return messageRepositoryService.ChatMessagesBetweenUsers(user1, user2, page, size);
    }

    /**
     * Gibt eine Liste der Chat-Teilnehmer eines Benutzers zurück.
     *
     * @param user1 der Benutzer
     * @return eine Liste von Benutzern
     */
    public List<UserEntity> getChatParticipants(Long user1) {
        return messageRepositoryService.ChatParticipants(user1);
    }

    /**
     * Sendet eine Direktnachricht von einem Benutzer an einen anderen Benutzer.
     *
     * @param senderId die ID des Absenders
     * @param recipientId die ID des Empfängers
     * @param content der Inhalt der Nachricht
     * @return die gesendete Nachricht
     */
    public MessageEntity sendMessage(Long senderId, Long recipientId, String content) {
        UserEntity sender = userRepositoryService.findUserById(senderId);
        UserEntity recipient = userRepositoryService.findUserById(recipientId);

        DirectChat directChat = direktChatRepositoryService.findDirectChatBetweenUsers(senderId, recipientId);

        MessageEntity message = new MessageEntity();
        message.setSender(sender);
        message.setRecipient(recipient);
        message.setContent(content);
        message.setTimestamp(LocalDateTime.now());
        message.setDirectChat(directChat);

        return messageRepositoryService.saveMessage(message);
    }

    /**
     * Sendet eine Gruppen-Nachricht an alle Mitglieder einer bestimmten Gruppe.
     *
     * @param senderId die ID des Absenders
     * @param groupId die ID der Gruppe
     * @param content der Inhalt der Nachricht
     * @return die gesendete Gruppen-Nachricht
     */
    public MessageEntity sendGroupMessage(Long senderId, Long groupId, String content) {
        UserEntity sender = userRepositoryService.findUserById(senderId);
        ChatGroup group = chatGroupRepositoryService.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Gruppe nicht gefunden"));

        MessageEntity message = new MessageEntity();
        message.setSender(sender);
        message.setChatGroup(group);
        message.setContent(content);
        message.setTimestamp(LocalDateTime.now());

        return messageRepositoryService.saveMessage(message);
    }

    /**
     * Gibt eine Liste der Nachrichten einer Gruppe zurück.
     *
     * @param groupId die ID der Gruppe
     * @param page die Seitenzahl für die Paginierung
     * @param size die Seitengröße für die Paginierung
     * @return eine Liste von Nachrichten
     */
    public List<MessageEntity> getGroupMessages(Long groupId, int page, int size) {
        return messageRepositoryService.findChatParticipants(groupId, page, size);
    }

    /**
     * Gibt alle Chat-Gruppen eines Benutzers zurück.
     *
     * @param userId die ID des Benutzers
     * @return eine Liste von Chat-Gruppen
     */
    public List<ChatGroup> getChatGroups(Long userId) {
        return chatGroupRepositoryService.findChatGroups(userId);
    }

    /**
     * Gibt alle Direktchats eines Benutzers zurück.
     *
     * @param userId die ID des Benutzers
     * @return eine Liste von Direktchats
     */
    public List<DirectChat> getDirectChats(Long userId) {
        return direktChatRepositoryService.findDirectChats(userId);
    }

    /**
     * Gibt den Direktchat zwischen zwei Benutzern zurück.
     *
     * @param user1Id die ID des ersten Benutzers
     * @param user2Id die ID des zweiten Benutzers
     * @return den entsprechenden Direktchat
     */
    public DirectChat getDirectChat(Long user1Id, Long user2Id) {
        return direktChatRepositoryService.findDirectChatBetweenUsers(user1Id, user2Id);
    }

    /**
     * Erstellt einen neuen Chat (entweder eine Direktnachricht oder eine Gruppe) für die ausgewählten Benutzer.
     *
     * @param selectedUsers die Liste der ausgewählten Benutzer
     * @param chatName der Name des Chats (nur für Gruppen)
     * @param currentUser der aktuell angemeldete Benutzer
     */
    public void createChat(List<Long> selectedUsers, String chatName, UserEntity currentUser) {
        if (selectedUsers.size() > 1) {
            if (!selectedUsers.contains(currentUser.getId())) {
                selectedUsers.add(currentUser.getId());
            }

            List<UserEntity> users = userRepositoryService.findUsersByIds(selectedUsers);

            Optional<ChatGroup> existingGroup = chatGroupRepositoryService.findByUsersInAndName(users, chatName);
            if (existingGroup.isPresent()) {
                existingGroup.get();
            } else {
                ChatGroup chatGroup = new ChatGroup();
                chatGroup.setName(chatName);
                chatGroup.setUsers(users);

                chatGroupRepositoryService.saveChatGroup(chatGroup);
            }
        } else {
            Long selectedUserId = selectedUsers.get(0);

            if (selectedUserId.equals(currentUser.getId())) {
                return;
            }

            UserEntity otherUser = userRepositoryService.findUserById(selectedUserId);

            DirectChat existingDirectChat = direktChatRepositoryService.findDirectChatBetweenUsers(currentUser.getId(), otherUser.getId());

            if (existingDirectChat != null) {
            } else {
                DirectChat directChat = new DirectChat();
                directChat.setUser1(currentUser);
                directChat.setUser2(otherUser);

                direktChatRepositoryService.saveDirectChat(directChat);
            }
        }
    }

    /**
     * Gibt den Chat mit der angegebenen ID zurück (kann entweder ein Gruppenchat oder ein Direktchat sein).
     *
     * @param chatId die ID des Chats
     * @return den Chat, entweder eine Gruppe oder ein Direktchat
     */
    public Object getChatById(Long chatId) {
        Optional<ChatGroup> chatGroup = chatGroupRepositoryService.findById(chatId);
        if (chatGroup.isPresent()) {
            return chatGroup.get();
        }

        Optional<DirectChat> directChat = direktChatRepositoryService.findById(chatId);
        if (directChat.isPresent()) {
            return directChat.get();
        }

        throw new IllegalArgumentException("Chat nicht gefunden");
    }

    /**
     * Löscht einen Gruppenchat, wenn der aktuelle Benutzer berechtigt ist.
     *
     * @param group die zu löschende Gruppe
     * @param currentUser der aktuell angemeldete Benutzer
     * @return true, wenn die Gruppe gelöscht wurde, andernfalls false
     */
    public boolean deleteGroupChat(ChatGroup group, UserEntity currentUser) {
        if (group.getUsers().contains(currentUser)) {
            chatGroupRepositoryService.deleteChatGroup(group);
        } else {
            throw new IllegalStateException("Du bist nicht berechtigt, diese Gruppe zu löschen.");
        }
        return true;
    }

    /**
     * Löscht einen Direktchat, wenn der aktuelle Benutzer berechtigt ist.
     *
     * @param chat der zu löschende Direktchat
     * @param currentUser der aktuell angemeldete Benutzer
     * @return true, wenn der Direktchat gelöscht wurde, andernfalls false
     */
    public boolean deleteDirectChat(DirectChat chat, UserEntity currentUser) {
        if (chat.getUser1().equals(currentUser) || chat.getUser2().equals(currentUser)) {
            direktChatRepositoryService.deleteDirectChat(chat);
        } else {
            throw new IllegalStateException("Du bist nicht berechtigt, diesen Direkt-Chat zu löschen.");
        }
        return true;
    }

    /**
     * Gibt die letzte Nachricht eines Direktchats zurück.
     *
     * @param chatId die ID des Direktchats
     * @return die letzte Nachricht des Direktchats
     */
    public MessageEntity getLastMessage(Long chatId) {
        return messageRepositoryService.getLastDirectChatMessage(chatId);
    }

    /**
     * Gibt die letzte Nachricht eines Gruppen-Chats zurück.
     *
     * @param groupId die ID der Gruppe
     * @return die letzte Nachricht der Gruppe
     */
    public MessageEntity getLastGroupMessage(Long groupId) {
        return messageRepositoryService.getLastGroupChatMessage(groupId);
    }

    /**
     * Bereitet die öffentliche Benutzerseite für einen Benutzer vor und fügt sie dem Model hinzu.
     *
     * @param model das Model, das an die Ansicht übergeben wird
     * @param userId die ID des Benutzers
     */
    public void preparePublicUsersPage(Model model, Long userId) {
        List<UserEntity> users = userService.getUsersWithFriendsOrPublic(userId);

        Map<Character, List<UserEntity>> groupedUsers = users.stream()
                .collect(Collectors.groupingBy(user -> user.getUsername().charAt(0)));

        model.addAttribute("publicUsers", groupedUsers);
    }

    /**
     * Filtert die Chats eines Benutzers basierend auf einer Suchanfrage.
     *
     * @param senderId die ID des Benutzers, der die Anfrage stellt
     * @param query der Suchbegriff
     * @return eine Liste von gefilterten Chats
     */
    public List<Object> filterChats(Long senderId, String query) {
        List<Object> filteredChatList = new ArrayList<>();

        if (query != null && !query.trim().isEmpty()) {
            List<UserEntity> filteredUsers = userService.findByUsername(query);

            for (UserEntity filteredUser : filteredUsers) {
                DirectChat directChat = getDirectChat(senderId, filteredUser.getId());
                if (directChat != null) {
                    filteredChatList.add(directChat);
                }
            }

            List<ChatGroup> filteredChatGroups = chatGroupRepositoryService.findChatGroupsContaining(query);

            for (ChatGroup chatGroup : filteredChatGroups) {
                if (getChatGroups(senderId).contains(chatGroup)) {
                    filteredChatList.add(chatGroup);
                }
            }
        } else {
            filteredChatList.addAll(getDirectChats(senderId));
            filteredChatList.addAll(getChatGroups(senderId));
        }

        return filteredChatList;
    }

    /**
     * Bereitet die Details für einen bestimmten Chat vor (entweder eine Gruppe oder ein Direktchat).
     *
     * @param senderId die ID des Absenders
     * @param groupId die ID der Gruppe
     * @param user2 die ID des zweiten Benutzers (für Direktchats)
     * @param query der Suchbegriff
     * @return ein DTO mit den Chat-Details
     */
    public ChatDetailsDTO prepareChatDetails(Long senderId, Long groupId, Long user2, String query) {
        List<MessageEntity> messages = null;
        UserEntity selectedUser = null;
        ChatGroup selectedGroup = null;
        DirectChat selectedDirectChat = null;
        String otherUserName = null;
        String otherUserPicture = null;
        Long chatId = null;
        Long recipientId = null;

        if (groupId != null) {
            selectedGroup = chatGroupRepositoryService.findById(groupId).orElse(null);
            messages = messageRepositoryService.findChatParticipants(groupId, 0, 50);
            chatId = groupId;
        } else if (user2 != null) {
            selectedUser = userRepositoryService.findUserById(user2);
            selectedDirectChat = direktChatRepositoryService.findDirectChatBetweenUsers(senderId, user2);
            messages = messageRepositoryService.ChatMessagesBetweenUsers(senderId, user2, 0, 50);

            if (selectedUser != null) {
                otherUserName = selectedUser.getUsername();
                otherUserPicture = selectedUser.getProfilePictureUrl();
            }

            recipientId = user2;
            if (selectedDirectChat != null) {
                chatId = selectedDirectChat.getId();
            }
        }
        return new ChatDetailsDTO(selectedGroup, selectedUser, selectedDirectChat, messages, otherUserName, otherUserPicture, groupId, chatId, recipientId);
    }

    /**
     * Bereitet eine Liste von Chat-Details für die Anzeige vor.
     *
     * @param filteredChatList die Liste der gefilterten Chats
     * @return eine Liste von ChatDetails-Objekten
     */
    public List<ChatDetail> prepareChatDetailsList(List<Object> filteredChatList) {
        List<ChatDetail> chatDetailsList = new ArrayList<>();

        for (Object chat : filteredChatList) {
            String lastMessageContent = "No messages yet";
            LocalDateTime lastMessageTime = null;

            if (chat instanceof DirectChat) {
                DirectChat directChat = (DirectChat) chat;
                MessageEntity lastMessage = getLastMessage(directChat.getId());
                if (lastMessage != null) {
                    lastMessageContent = lastMessage.getContent();
                    lastMessageTime = lastMessage.getTimestamp();
                }
                chatDetailsList.add(new ChatDetail(directChat, lastMessageContent, lastMessageTime));
            } else if (chat instanceof ChatGroup) {
                ChatGroup chatGroup = (ChatGroup) chat;
                MessageEntity lastMessage = getLastGroupMessage(chatGroup.getId());
                if (lastMessage != null) {
                    lastMessageContent = lastMessage.getContent();
                    lastMessageTime = lastMessage.getTimestamp();
                }
                chatDetailsList.add(new ChatDetail(chatGroup, lastMessageContent, lastMessageTime));
            }
        }
        return chatDetailsList;
    }

    /**
     * Verarbeitet das Senden einer Nachricht.
     *
     * @param messageDTO das DTO mit den Nachrichtendaten
     */
    public void handleMessageSending(MessageDTO messageDTO) {
        // Überprüfen, ob die Nachricht gültig ist
        if (messageDTO.getContent() == null || messageDTO.getContent().isEmpty()) {
            throw new IllegalArgumentException("Nachrichteninhalt darf nicht leer sein.");
        }

        MessageEntity savedMessage;

        if (messageDTO.getChatGroupId() != null) {
            // Gruppen-Nachricht senden
            UserEntity sender = userRepositoryService.findUserById(messageDTO.getSenderId());
            savedMessage = sendGroupMessage(messageDTO.getSenderId(), messageDTO.getChatGroupId(), messageDTO.getContent());
            savedMessage.setSender(sender);
            brokerMessagingTemplate.convertAndSend("/topic/messages/group/" + messageDTO.getChatGroupId(), savedMessage);
        } else if (messageDTO.getRecipientId() != null) {
            // Direktnachricht senden
            savedMessage = sendMessage(messageDTO.getSenderId(), messageDTO.getRecipientId(), messageDTO.getContent());
            brokerMessagingTemplate.convertAndSend("/topic/messages/" + messageDTO.getRecipientId(), savedMessage);
        } else {
            throw new IllegalArgumentException("Weder Empfänger noch Gruppen-ID angegeben.");
        }

    }

    /**
     * Bereitet die öffentliche Benutzerseite vor und gibt die Mitglieder als Liste zurück.
     *
     * @param userId die ID des Benutzers
     * @return eine Liste von Benutzern, die Mitglieder sind
     */
    public List<UserEntity> preparePublicUsersPage(Long userId) {
        List<UserEntity> publicUsers = userService.getPublicUsers();

        List<UserEntity> members = publicUsers.stream()
                .filter(UserEntity::isMember)
                .toList();

        return members;
    }

    /**
     * Gruppiert eine Liste von Benutzern nach dem ersten Buchstaben ihres Benutzernamens.
     *
     * @param users die Liste der Benutzer
     * @return eine Map, die Benutzer nach dem ersten Buchstaben ihres Benutzernamens gruppiert
     */
    public Map<Character, List<UserEntity>> groupUsersByInitial(List<UserEntity> users) {
        return users.stream()
                .collect(Collectors.groupingBy(user -> Character.toUpperCase(user.getUsername().charAt(0))));
    }

    /**
     * Validiert die Anfrage zum Erstellen einer Gruppe.
     *
     * @param request die Anfrage zum Erstellen einer Gruppe
     * @param model das Model, das die Fehlernachricht enthält
     * @param currentUser der aktuell angemeldete Benutzer
     * @return true, wenn die Anfrage gültig ist, andernfalls false
     */
    public boolean validateCreateGroupRequest(CreateGroupRequest request, Model model, UserEntity currentUser) {
        if (request == null) {
            model.addAttribute("errorMessage", "Ungültige Anfrage. Bitte versuchen Sie es erneut.");
            preparePublicUsersPage(model, currentUser.getId());
            return false;
        }

        List<Long> selectedUsers = request.getSelectedUsers();
        String chatName = request.getChatName();

        if (selectedUsers == null || selectedUsers.isEmpty()) {
            model.addAttribute("errorMessage", "Bitte wählen Sie mindestens einen Benutzer aus.");
            preparePublicUsersPage(model, currentUser.getId());
            return false;
        }

        if (selectedUsers.size() > 1 && (chatName == null || chatName.trim().isEmpty())) {
            model.addAttribute("errorMessage", "Bitte geben Sie einen Gruppennamen ein, wenn Sie mehrere Benutzer auswählen.");
            preparePublicUsersPage(model, currentUser.getId());
            return false;
        }

        return true;
    }

    /**
     * Löscht einen Chat anhand seiner ID, je nach Typ (Gruppe oder Direktchat).
     *
     * @param chatId die ID des zu löschenden Chats
     * @param currentUser der aktuell angemeldete Benutzer
     * @return true, wenn der Chat gelöscht wurde, andernfalls false
     */
    public boolean deleteChatById(Long chatId, UserEntity currentUser) {
        Object chat = getChatById(chatId);
        if (chat == null) {
            return false; // Chat nicht gefunden
        }

        if (chat instanceof ChatGroup) {
            return deleteGroupChat((ChatGroup) chat, currentUser);
        } else if (chat instanceof DirectChat) {
            return deleteDirectChat((DirectChat) chat, currentUser);
        }
        return false; // Weder Gruppenchat noch Direktchat
    }

    public ChatResponseDTO getChatData(Long user2, Long groupId, String query) {
        UserEntity currentUser = userService.getCurrentUser();

        Long senderId = currentUser.getId();

        // Initialisiere das ResponseDTO
        ChatResponseDTO responseDTO = new ChatResponseDTO();
        responseDTO.setCurrentUser(currentUser.getUsername());
        responseDTO.setSenderId(senderId);

        // Sammle allgemeine Chatdaten und füge sie zum DTO hinzu
        responseDTO.setChatGroups(getChatGroups(senderId));
        responseDTO.setDirectChats(getDirectChats(senderId));
        responseDTO.setChatParticipants(getChatParticipants(senderId));
        responseDTO.setFilteredChatList(filterChats(senderId, query));
        responseDTO.setChatDetails(prepareChatDetailsList(responseDTO.getFilteredChatList()));

        // Bereite Chat-Details vor und füge sie zum DTO hinzu
        if (groupId != null) {
            ChatGroup selectedGroup = chatGroupRepositoryService.findById(groupId).orElse(null);
            responseDTO.setSelectedGroup(selectedGroup);
            responseDTO.setMessages(messageRepositoryService.findChatParticipants(groupId, 0, 50));
            responseDTO.setChatId(groupId);
        } else if (user2 != null) {
            UserEntity selectedUser = userRepositoryService.findUserById(user2);
            DirectChat selectedDirectChat = direktChatRepositoryService.findDirectChatBetweenUsers(senderId, user2);

            responseDTO.setSelectedUser(selectedUser);
            responseDTO.setSelectedDirectChat(selectedDirectChat);
            responseDTO.setMessages(messageRepositoryService.ChatMessagesBetweenUsers(senderId, user2, 0, 50));

            if (selectedUser != null) {
                responseDTO.setOtherUserName(selectedUser.getUsername());
                responseDTO.setOtherUserPicture(selectedUser.getProfilePictureUrl());
            }

            responseDTO.setRecipientId(user2);
            if (selectedDirectChat != null) {
                responseDTO.setChatId(selectedDirectChat.getId());
            }
        }

        responseDTO.setGroupId(groupId);
        responseDTO.setQuery(query);


        return responseDTO;
    }

    public NewChatResponseDTO newChat(String currentUrl){
        NewChatResponseDTO newChatResponseDTO = new NewChatResponseDTO();
        newChatResponseDTO.setCurrentUrl(currentUrl);
        newChatResponseDTO.setGroupedUsers(groupUsersByInitial(preparePublicUsersPage(userService.getCurrentUser().getId())));
        return newChatResponseDTO;
    }




}
