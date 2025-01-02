package com.springboot.vitalorganize.service;

import com.springboot.vitalorganize.dto.ChatDetail;
import com.springboot.vitalorganize.dto.ChatDetailsDTO;
import com.springboot.vitalorganize.dto.CreateGroupRequest;
import com.springboot.vitalorganize.dto.MessageDTO;
import com.springboot.vitalorganize.model.*;
import com.springboot.vitalorganize.service.repositoryhelper.ChatGroupRepositoryService;
import com.springboot.vitalorganize.service.repositoryhelper.DirektChatRepositoryService;
import com.springboot.vitalorganize.service.repositoryhelper.MessageRepositoryService;
import com.springboot.vitalorganize.service.repositoryhelper.UserRepositoryService;
import lombok.AllArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ChatService {

    private final UserRepositoryService userRepositoryService;
    private final MessageRepositoryService messageRepositoryService;
    private final DirektChatRepositoryService direktChatRepositoryService;
    private final ChatGroupRepositoryService chatGroupRepositoryService;
    private SimpMessagingTemplate brokerMessagingTemplate;
    private final UserService userService;

    public List<MessageEntity> getMessages(Long user1, Long user2, int page, int size) {
        return messageRepositoryService.ChatMessagesBetweenUsers(user1, user2, page, size);
    }

    public List<UserEntity> getChatParticipants(Long user1) {
        return messageRepositoryService.ChatParticipants(user1);
    }

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

    public List<MessageEntity> getGroupMessages(Long groupId, int page, int size) {
        return messageRepositoryService.findChatParticipants(groupId, page, size);
    }

    public List<ChatGroup> getChatGroups(Long userId) {
        return chatGroupRepositoryService.findChatGroups(userId);
    }

    public List<DirectChat> getDirectChats(Long userId) {
        return direktChatRepositoryService.findDirectChats(userId);
    }

    public DirectChat getDirectChat(Long user1Id, Long user2Id) {
        return direktChatRepositoryService.findDirectChatBetweenUsers(user1Id, user2Id);
    }


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

    public boolean deleteGroupChat(ChatGroup group, UserEntity currentUser) {
        if (group.getUsers().contains(currentUser)) {
            chatGroupRepositoryService.deleteChatGroup(group);
        } else {
            throw new IllegalStateException("Du bist nicht berechtigt, diese Gruppe zu löschen.");
        }
        return true;
    }

    public boolean deleteDirectChat(DirectChat chat, UserEntity currentUser) {
        if (chat.getUser1().equals(currentUser) || chat.getUser2().equals(currentUser)) {
            direktChatRepositoryService.deleteDirectChat(chat);
        } else {
            throw new IllegalStateException("Du bist nicht berechtigt, diesen Direkt-Chat zu löschen.");
        }
        return true;
    }

    public MessageEntity getLastMessage(Long chatId) {
        return messageRepositoryService.getLastDirectChatMessage(chatId);
    }

    public MessageEntity getLastGroupMessage(Long groupId) {
        return messageRepositoryService.getLastGroupChatMessage(groupId);
    }

    public void preparePublicUsersPage(Model model, Long userId) {
        List<UserEntity> users = userService.getUsersWithFriendsOrPublic(userId);

        Map<Character, List<UserEntity>> groupedUsers = users.stream()
                .collect(Collectors.groupingBy(user -> user.getUsername().charAt(0)));

        model.addAttribute("publicUsers", groupedUsers);
    }

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

    public List<UserEntity> preparePublicUsersPage(Long userId) {
        List<UserEntity> publicUsers = userService.getPublicUsers();

        List<UserEntity> members = publicUsers.stream()
                .filter(UserEntity::isMember)
                .toList();

        return members;
    }

    public Map<Character, List<UserEntity>> groupUsersByInitial(List<UserEntity> users) {
        return users.stream()
                .collect(Collectors.groupingBy(user -> Character.toUpperCase(user.getUsername().charAt(0))));
    }

    public boolean validateCreateGroupRequest(CreateGroupRequest request, Model model, UserEntity currentUser) {
        if (request == null) {
            model.addAttribute("errorMessage", "Ungültige Anfrage. Bitte versuchen Sie es erneut.");
            preparePublicUsersPage(model, currentUser.getId());
            return false;
        }

        List<Long> selectedUsers = request.getSelectedUsers();
        System.out.println(selectedUsers);
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



}