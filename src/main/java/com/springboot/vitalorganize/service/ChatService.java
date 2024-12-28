package com.springboot.vitalorganize.service;

import com.springboot.vitalorganize.model.*;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ChatService {

    private final UserRepository userRepository;

    private final MessageRepository messageRepository;

    private final ChatGroupRepository chatGroupRepository;

    private final DirectChatRepository directChatRepository;
    private final UserService userService;
    private final JavaMailSender javaMailSender;


    private Pageable createPageable(int page, int size, String sortBy, Sort.Direction direction) {
        return PageRequest.of(page, size, Sort.by(direction, sortBy));
    }

    public List<MessageEntity> getMessages(Long user1, Long user2, int page, int size) {
        Pageable pageable = createPageable(page, size, "timestamp", Sort.Direction.DESC);
        return messageRepository.findChatMessages(user1, user2, pageable).getContent();
    }

    public List<UserEntity> getChatParticipants(Long user1) {
        return messageRepository.findChatParticipants(user1);
    }

    public MessageEntity sendMessage(Long senderId, Long recipientId, String content) {
        UserEntity sender = userRepository.findById(senderId)
                .orElseThrow(() -> new IllegalArgumentException("Sender nicht gefunden"));
        UserEntity recipient = userRepository.findById(recipientId)
                .orElseThrow(() -> new IllegalArgumentException("Empfänger nicht gefunden"));

        DirectChat directChat = directChatRepository.findByUser1IdAndUser2Id(senderId, recipientId);
        if(directChat == null) {
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

    public MessageEntity sendGroupMessage(Long senderId, Long groupId, String content) {
        UserEntity sender = userRepository.findById(senderId)
                .orElseThrow(() -> new IllegalArgumentException("Sender nicht gefunden"));
        ChatGroup group = chatGroupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Gruppe nicht gefunden"));

        MessageEntity message = new MessageEntity();
        message.setSender(sender);
        message.setChatGroup(group);
        message.setContent(content);
        message.setTimestamp(LocalDateTime.now());

        return messageRepository.save(message);
    }

    public List<MessageEntity> getGroupMessages(Long groupId, int page, int size) {
        Pageable pageable = createPageable(page, size, "timestamp", Sort.Direction.ASC);
        return messageRepository.findByChatGroup_Id(groupId, pageable);
    }

    public List<ChatGroup> getChatGroups(Long userId) {
        // Benutze das ChatGroupRepository, um alle Gruppen zu finden, in denen der Benutzer Mitglied ist.
        return chatGroupRepository.findByUsers_Id(userId);
    }

    public List<DirectChat> getDirectChats(Long userId) {
        return directChatRepository.findByUser1IdOrUser2Id(userId, userId);
    }

    public DirectChat getDirectChat(Long user1Id, Long user2Id) {
        DirectChat chat = directChatRepository.findByUser1IdAndUser2Id(user1Id, user2Id);
        if (chat == null) {
            chat = directChatRepository.findByUser2IdAndUser1Id(user1Id, user2Id);
        }
        return chat;
    }


    public void createChat(List<Long> selectedUsers, String chatName, UserEntity currentUser) {
        // Wenn mehr als ein Benutzer für die Gruppe ausgewählt wurde
        if (selectedUsers.size() > 1) {
            // Sicherstellen, dass der aktuelle Benutzer (currentUser) in der Liste enthalten ist
            if (!selectedUsers.contains(currentUser.getId())) {
                selectedUsers.add(currentUser.getId()); // Aktuellen Benutzer zur Liste hinzufügen
            }

            // Holen der User-Entitäten basierend auf den IDs
            List<UserEntity> users = userRepository.findAllById(selectedUsers);

            // Überprüfen, ob bereits eine Chat-Gruppe mit denselben Benutzern existiert
            Optional<ChatGroup> existingGroup = chatGroupRepository.findByUsersInAndName(users, chatName);
            if (existingGroup.isPresent()) {
                // Wenn die Gruppe bereits existiert, gib die bestehende Gruppe zurück
                existingGroup.get();
            } else {
                // Wenn keine Gruppe existiert, erstelle eine neue
                ChatGroup chatGroup = new ChatGroup();
                chatGroup.setName(chatName);
                chatGroup.setUsers(users);

                chatGroupRepository.save(chatGroup);
            }
        } else {
            // Wenn nur ein Benutzer ausgewählt wurde, also ein Direkt-Chat erstellt wird
            Long selectedUserId = selectedUsers.get(0);

            // Prüfen, ob der Benutzer versucht, einen Direkt-Chat mit sich selbst zu erstellen
            if (selectedUserId.equals(currentUser.getId())) {
                // Weiterleitung zurück zur Chat-Seite, wenn der Benutzer sich selbst auswählt
                return;
            }

            // Prüfen, ob bereits ein Direkt-Chat zwischen den beiden Benutzern existiert
            UserEntity otherUser = userRepository.findUserEntityById(selectedUserId);

            // Überprüfen, ob der Direkt-Chat zwischen den beiden Benutzern existiert
            DirectChat existingDirectChat = directChatRepository.findByUser1IdAndUser2Id(currentUser.getId(), otherUser.getId());
            if (existingDirectChat == null) {
                existingDirectChat = directChatRepository.findByUser2IdAndUser1Id(currentUser.getId(), otherUser.getId());
            }

            if (existingDirectChat != null) {
                // Wenn der Direkt-Chat bereits existiert, gib ihn zurück
            } else {
                // Wenn kein Direkt-Chat existiert, erstelle einen neuen
                DirectChat directChat = new DirectChat();
                directChat.setUser1(currentUser);
                directChat.setUser2(otherUser);

                directChatRepository.save(directChat);
            }
        }
    }


    public Object getChatById(Long chatId) {
        // Versuche, einen Gruppenchat zu finden
        Optional<ChatGroup> chatGroup = chatGroupRepository.findById(chatId);
        if (chatGroup.isPresent()) {
            return chatGroup.get();
        }

        // Versuche, einen Direkt-Chat zu finden
        Optional<DirectChat> directChat = directChatRepository.findById(chatId);
        if (directChat.isPresent()) {
            return directChat.get();
        }

        throw new IllegalArgumentException("Chat nicht gefunden");
    }

    public void deleteGroupChat(ChatGroup group, UserEntity currentUser) {
        // Prüfen, ob der aktuelle Benutzer Mitglied der Gruppe ist
        if (group.getUsers().contains(currentUser)) {
            chatGroupRepository.delete(group); // Lösche die Gruppe
        } else {
            throw new IllegalStateException("Du bist nicht berechtigt, diese Gruppe zu löschen.");
        }
    }

    public void deleteDirectChat(DirectChat chat, UserEntity currentUser) {
        // Prüfen, ob der aktuelle Benutzer in dem Direkt-Chat ist
        if (chat.getUser1().equals(currentUser) || chat.getUser2().equals(currentUser)) {
            directChatRepository.delete(chat); // Lösche den Direkt-Chat
        } else {
            throw new IllegalStateException("Du bist nicht berechtigt, diesen Direkt-Chat zu löschen.");
        }
    }

    public MessageEntity getLastMessage(Long id) {
        Pageable pageable = PageRequest.of(0, 1, Sort.by(Sort.Order.desc("timestamp")));
        List<MessageEntity> messages = messageRepository.findLastMessageForDirectChat(id, pageable);
        MessageEntity message = messages.isEmpty() ? null : messages.get(0);
        return message;
    }

    public MessageEntity getLastGroupMessage(Long id) {
        Pageable pageable = PageRequest.of(0, 1, Sort.by(Sort.Order.desc("timestamp")));
        List<MessageEntity> messages = messageRepository.findLastMessageForChatGroup(id, pageable);
        MessageEntity message = messages.isEmpty() ? null : messages.get(0);
        return message;
    }

    public void preparePublicUsersPage(Model model, String theme, String lang, Long userId) {
        List<UserEntity> users = getUsersWithFriendsOrPublic(userId);

        Map<Character, List<UserEntity>> groupedUsers = users.stream()
                .collect(Collectors.groupingBy(user -> user.getUsername().charAt(0)));

        model.addAttribute("publicUsers", groupedUsers);
        model.addAttribute("themeCss", userService.getThemeCss(theme));
        model.addAttribute("lang", lang);
    }

    public List<UserEntity> getUsersWithFriendsOrPublic(Long userId) {
        UserEntity currentUser = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

        // Finde alle öffentlichen Benutzer
        List<UserEntity> publicUsers = userRepository.findAllByisPublic(true);

        // Finde alle Freunde des aktuellen Benutzers
        List<UserEntity> friends = currentUser.getFriends();

        // Kombiniere die Listen der öffentlichen Benutzer und Freunde
        publicUsers.addAll(friends);

        // Entferne Duplikate (falls der Benutzer sowohl in der Freundesliste als auch als öffentlich erscheint)
        return publicUsers.stream().distinct().collect(Collectors.toList());
    }
}