package com.springboot.vitalorganize.service;

import com.springboot.vitalorganize.component.PaginationHelper;
import com.springboot.vitalorganize.entity.Chat.ChatGroupEntity;
import com.springboot.vitalorganize.entity.Chat.DirectChatEntity;
import com.springboot.vitalorganize.entity.Chat.MessageEntity;
import com.springboot.vitalorganize.entity.Profile_User.UserEntity;
import com.springboot.vitalorganize.model.Chat.*;
import com.springboot.vitalorganize.repository.ChatGroupRepository;
import com.springboot.vitalorganize.repository.DirectChatRepository;
import com.springboot.vitalorganize.repository.MessageRepository;
import com.springboot.vitalorganize.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final DirectChatRepository directChatRepository;
    private final ChatGroupRepository chatGroupRepository;

    private SimpMessagingTemplate brokerMessagingTemplate;
    private final UserService userService;
    private final PaginationHelper paginationHelper;


    /**
     * Gibt eine Liste der Chat-Teilnehmer eines Benutzers zurück.
     *
     * @param userId der Benutzer
     * @return eine Liste von Benutzern
     */
    public List<UserEntity> getChatParticipants(Long userId) {
        List<MessageEntity> messages = messageRepository.findBySenderIdOrRecipientId(userId, userId);

        List<UserEntity> participants = new ArrayList<>();

        for (MessageEntity message : messages) {
            if (message.getSender() != null && !Objects.equals(message.getSender().getId(), userId)) {
                participants.add(message.getSender());
            }
            if (message.getRecipient() != null && !Objects.equals(message.getRecipient().getId(), userId)) {
                participants.add(message.getRecipient());
            }
        }

        return participants;
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
        UserEntity sender = userRepository.findUserEntityById(senderId);
        UserEntity recipient = userRepository.findUserEntityById(recipientId);

        DirectChatEntity directChat = directChatRepository.findByUser1IdAndUser2Id(senderId, recipientId);
        if (directChat == null) {
            directChat = directChatRepository.findByUser2IdAndUser1Id(senderId, recipientId);
        }

        MessageEntity message = new MessageEntity();
        message.setSender(sender);
        message.setRecipient(recipient);
        message.setContent(content);
        message.setTimestamp(LocalDateTime.now());
        message.setDirectChat(directChat);

        return messageRepository.save(message);
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
        UserEntity sender = userRepository.findUserEntityById(senderId);
        ChatGroupEntity group = chatGroupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Gruppe nicht gefunden"));

        MessageEntity message = new MessageEntity();
        message.setSender(sender);
        message.setChatGroup(group);
        message.setContent(content);
        message.setTimestamp(LocalDateTime.now());

        return messageRepository.save(message);
    }

    /**
     * Gibt alle Chat-Gruppen eines Benutzers zurück.
     *
     * @param userId die ID des Benutzers
     * @return eine Liste von Chat-Gruppen
     */
    public List<ChatGroupEntity> getChatGroups(Long userId) {
        return chatGroupRepository.findByUsers_Id(userId);
    }

    /**
     * Gibt alle Direktchats eines Benutzers zurück.
     *
     * @param userId die ID des Benutzers
     * @return eine Liste von Direktchats
     */
    public List<DirectChatEntity> getDirectChats(Long userId) {
        return directChatRepository.findByUser1IdOrUser2Id(userId, userId);
    }

    /**
     * Gibt den Direktchat zwischen zwei Benutzern zurück.
     *
     * @param user1Id die ID des ersten Benutzers
     * @param user2Id die ID des zweiten Benutzers
     * @return den entsprechenden Direktchat
     */
    public DirectChatEntity getDirectChat(Long user1Id, Long user2Id) {
        DirectChatEntity directChat = directChatRepository.findByUser1IdAndUser2Id(user1Id, user2Id);
        if (directChat == null) {
            directChat = directChatRepository.findByUser2IdAndUser1Id(user1Id, user2Id);
        }
        return directChat;
    }

    /**
     * Erstellt einen neuen Chat (entweder eine Direktnachricht oder eine Gruppe) für die ausgewählten Benutzer.
     *
     * @param selectedUsers die Liste der ausgewählten Benutzer
     * @param chatName der Name des Chats (nur für Gruppen)
     */
    public void createChat(List<Long> selectedUsers, String chatName) {
        UserEntity currentUser = userService.getCurrentUser();
        if (selectedUsers.size() > 1) {
            if (!selectedUsers.contains(currentUser.getId())) {
                selectedUsers.add(currentUser.getId());
            }

            List<UserEntity> users = userRepository.findAllById(selectedUsers);

            Optional<ChatGroupEntity> existingGroup = chatGroupRepository.findByUsersInAndName(users, chatName);
            if (existingGroup.isPresent()) {
                existingGroup.get();
            } else {
                ChatGroupEntity chatGroup = new ChatGroupEntity();
                chatGroup.setName(chatName);
                chatGroup.setUsers(users);

                chatGroupRepository.save(chatGroup);
            }
        } else {
            Long selectedUserId = selectedUsers.getFirst();

            if (selectedUserId.equals(currentUser.getId())) {
                return;
            }

            UserEntity otherUser = userRepository.findUserEntityById(selectedUserId);

            DirectChatEntity existingDirectChat = directChatRepository.findByUser1IdAndUser2Id(currentUser.getId(), otherUser.getId());
            if (existingDirectChat == null) {
                existingDirectChat = directChatRepository.findByUser2IdAndUser1Id(currentUser.getId(), otherUser.getId());
            }

            if (existingDirectChat != null) {
            } else {
                DirectChatEntity directChat = new DirectChatEntity();
                directChat.setUser1(currentUser);
                directChat.setUser2(otherUser);

                directChatRepository.save(directChat);
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
        Optional<ChatGroupEntity> chatGroup = chatGroupRepository.findById(chatId);
        if (chatGroup.isPresent()) {
            return chatGroup.get();
        }

        Optional<DirectChatEntity> directChat = directChatRepository.findById(chatId);
        if (directChat.isPresent()) {
            return directChat.get();
        }

        throw new IllegalArgumentException("Chat nicht gefunden");
    }

    /**
     * Löscht einen Gruppenchat
     *
     * @param group die zu löschende Gruppe
     * @param currentUser der aktuell angemeldete Benutzer
     * @return true, wenn die Gruppe gelöscht wurde, andernfalls false
     */
    public boolean deleteGroupChat(ChatGroupEntity group, UserEntity currentUser) {
        if (group.getUsers().contains(currentUser)) {
            chatGroupRepository.delete(group);
        } else {
            throw new IllegalStateException("Du bist nicht berechtigt, diese Gruppe zu löschen.");
        }
        return true;
    }

    /**
     * Löscht einen Direktchat
     *
     * @param chat der zu löschende Direktchat
     * @param currentUser der aktuell angemeldete Benutzer
     * @return true, wenn der Direktchat gelöscht wurde, andernfalls false
     */
    public boolean deleteDirectChat(DirectChatEntity chat, UserEntity currentUser) {
        if (chat.getUser1().equals(currentUser) || chat.getUser2().equals(currentUser)) {
            directChatRepository.delete(chat);
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
        Pageable pageable = PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<MessageEntity> messagesPage = messageRepository.findByDirectChatId(chatId, pageable);
        return messagesPage.getContent().isEmpty() ? null : messagesPage.getContent().getFirst();
    }

    /**
     * Gibt die letzte Nachricht eines Gruppen-Chats zurück.
     *
     * @param groupId die ID der Gruppe
     * @return die letzte Nachricht der Gruppe
     */
    public MessageEntity getLastGroupMessage(Long groupId) {
        Pageable pageable = PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<MessageEntity> messagesPage = messageRepository.findByChatGroupId(groupId, pageable);
        return messagesPage.getContent().isEmpty() ? null : messagesPage.getContent().getFirst();
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
                DirectChatEntity directChat = getDirectChat(senderId, filteredUser.getId());
                if (directChat != null) {
                    filteredChatList.add(directChat);
                }
            }

            List<ChatGroupEntity> filteredChatGroups = chatGroupRepository.findByNameContaining(query);

            for (ChatGroupEntity chatGroup : filteredChatGroups) {
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
     * Bereitet eine Liste von Chat-Details für die Anzeige vor.
     *
     * @param filteredChatList die Liste der gefilterten Chats
     * @return eine Liste von ChatDetails-Objekten
     */
    public List<ChatLastMessageInformationDTO> prepareChatDetailsList(List<Object> filteredChatList) {
        List<ChatLastMessageInformationDTO> chatDetailsList = new ArrayList<>();

        for (Object chat : filteredChatList) {
            String lastMessageContent = "No messages yet";
            LocalDateTime lastMessageTime = null;

            if (chat instanceof DirectChatEntity) {
                DirectChatEntity directChat = (DirectChatEntity) chat;
                MessageEntity lastMessage = getLastMessage(directChat.getId());
                if (lastMessage != null) {
                    lastMessageContent = lastMessage.getContent();
                    lastMessageTime = lastMessage.getTimestamp();
                }
                chatDetailsList.add(new ChatLastMessageInformationDTO(directChat, lastMessageContent, lastMessageTime));
            } else if (chat instanceof ChatGroupEntity) {
                ChatGroupEntity chatGroup = (ChatGroupEntity) chat;
                MessageEntity lastMessage = getLastGroupMessage(chatGroup.getId());
                if (lastMessage != null) {
                    lastMessageContent = lastMessage.getContent();
                    lastMessageTime = lastMessage.getTimestamp();
                }
                chatDetailsList.add(new ChatLastMessageInformationDTO(chatGroup, lastMessageContent, lastMessageTime));
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
            UserEntity sender = userRepository.findUserEntityById(messageDTO.getSenderId());
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
     * @return eine Liste von Benutzern, die Mitglieder sind
     */
    public List<UserEntity> preparePublicUsersPage() {
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
     * @return true, wenn die Anfrage gültig ist, andernfalls false
     */
    public boolean validateCreateGroupRequest(CreateChatGroupRequestDTO request, Model model) {
        UserEntity currentUser = userService.getCurrentUser();
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
     * @return true, wenn der Chat gelöscht wurde, andernfalls false
     */
    public boolean deleteChatById(Long chatId) {
        UserEntity currentUser = userService.getCurrentUser();
        Object chat = getChatById(chatId);
        if (chat == null) {
            return false; // Chat nicht gefunden
        }

        if (chat instanceof ChatGroupEntity) {
            return deleteGroupChat((ChatGroupEntity) chat, currentUser);
        } else if (chat instanceof DirectChatEntity) {
            return deleteDirectChat((DirectChatEntity) chat, currentUser);
        }
        return false; // Weder Gruppenchat noch Direktchat
    }


    /**
     * Holt die relevanten Chats des eingeloggten users entweder aus einem Gruppenchat oder aus einem direct Chat
     * @param chatRequestDTO Benötigte informationen
     * @return relevante Chats
     */
    public ChatResponseDTO getChatData(ChatRequestDTO chatRequestDTO) {
        UserEntity currentUser = userService.getCurrentUser();

        Long senderId = currentUser.getId();

        ChatResponseDTO responseDTO = new ChatResponseDTO();
        responseDTO.setCurrentUser(currentUser.getUsername());
        responseDTO.setSenderId(senderId);

        responseDTO.setChatGroups(getChatGroups(senderId));
        responseDTO.setDirectChats(getDirectChats(senderId));
        responseDTO.setChatParticipants(getChatParticipants(senderId));
        responseDTO.setFilteredChatList(filterChats(senderId, chatRequestDTO.getQuery()));
        responseDTO.setChatDetails(prepareChatDetailsList(responseDTO.getFilteredChatList()));

        if (chatRequestDTO.getGroup() != null) {
            ChatGroupEntity selectedGroup = chatGroupRepository.findById(chatRequestDTO.getGroup()).orElse(null);
            responseDTO.setSelectedGroup(selectedGroup);
            Sort sort = Sort.by(Sort.Direction.DESC, "timestamp");
            int pageNumber = chatRequestDTO.getPage();
            int pageSize = 20;
            Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);

            Page<MessageEntity> messagesPage = messageRepository.findByChatGroup_Id(chatRequestDTO.getGroup(), pageable);

            List<MessageEntity> allMessages = new ArrayList<>();

            if (chatRequestDTO.getPage() > 0) {
                Pageable previousPageable = PageRequest.of(0, pageSize * chatRequestDTO.getPage(), sort);
                Page<MessageEntity> previousMessages = messageRepository.findByChatGroup_Id(chatRequestDTO.getGroup(), previousPageable);
                allMessages.addAll(previousMessages.getContent());
            }

            allMessages.addAll(messagesPage.getContent());

            Collections.reverse(allMessages);


            responseDTO.setMessages(allMessages);
            responseDTO.setPage(chatRequestDTO.getPage());
            responseDTO.setTotalMessages(messagesPage.getTotalElements());
            responseDTO.setChatId(chatRequestDTO.getGroup());

        } else if (chatRequestDTO.getUser2() != null) {
            UserEntity selectedUser = userRepository.findUserEntityById(chatRequestDTO.getUser2());
            DirectChatEntity selectedDirectChat =  directChatRepository.findByUser1IdAndUser2Id(senderId, chatRequestDTO.getUser2());
            if (selectedDirectChat == null) {
                selectedDirectChat = directChatRepository.findByUser2IdAndUser1Id(senderId, chatRequestDTO.getUser2());
            }

            responseDTO.setSelectedUser(selectedUser);
            responseDTO.setSelectedDirectChat(selectedDirectChat);
            Sort sort = Sort.by(Sort.Direction.DESC, "timestamp");
            int pageNumber = chatRequestDTO.getPage();
            int pageSize = 20;
            Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);

            Page<MessageEntity> messagesPage = messageRepository.findBySenderIdAndRecipientIdOrRecipientIdAndSenderId(senderId, chatRequestDTO.getUser2(), senderId, chatRequestDTO.getUser2(), pageable);

            List<MessageEntity> allMessages = new ArrayList<>();

            if (chatRequestDTO.getPage() > 0) {
                Pageable previousPageable = PageRequest.of(0, pageSize * chatRequestDTO.getPage(), sort);
                Page<MessageEntity> previousMessages = messageRepository.findBySenderIdAndRecipientIdOrRecipientIdAndSenderId(
                        senderId, chatRequestDTO.getUser2(), senderId, chatRequestDTO.getUser2(), previousPageable);
                allMessages.addAll(previousMessages.getContent());
            }

            allMessages.addAll(messagesPage.getContent());

            Collections.reverse(allMessages);

            responseDTO.setMessages(allMessages);
            responseDTO.setPage(chatRequestDTO.getPage());
            responseDTO.setTotalMessages(messagesPage.getTotalElements());

            if (selectedUser != null) {
                responseDTO.setOtherUserName(selectedUser.getUsername());
                responseDTO.setOtherUserPicture(selectedUser.getProfilePictureUrl());
            }

            responseDTO.setRecipientId(chatRequestDTO.getUser2());
            if (selectedDirectChat != null) {
                responseDTO.setChatId(selectedDirectChat.getId());
            }
        }


        responseDTO.setGroupId(chatRequestDTO.getGroup());
        responseDTO.setQuery(chatRequestDTO.getQuery());
        responseDTO.setRecipientId(chatRequestDTO.getUser2());


        return responseDTO;
    }

    /**
     * Gibt alle user zurück, mit denen ein neuer Chat erstellt werden kann.
     * @param currentUrl Aktuelle Url
     * @return Alle User
     */
    public NewChatResponseDTO newChat(String currentUrl){
        NewChatResponseDTO newChatResponseDTO = new NewChatResponseDTO();
        newChatResponseDTO.setCurrentUrl(currentUrl);
        newChatResponseDTO.setGroupedUsers(groupUsersByInitial(preparePublicUsersPage()));
        return newChatResponseDTO;
    }




}
