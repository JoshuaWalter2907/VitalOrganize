package com.springboot.vitalorganize.service;

import com.springboot.vitalorganize.model.*;
import lombok.AllArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class ChatService {

    private final UserRepository userRepository;

    private final MessageRepository messageRepository;

    private final ChatGroupRepository chatGroupRepository;

    private final DirectChatRepository directChatRepository;

    public List<MessageEntity> getMessages(Long user1, Long user2, int page, int size) {
        System.out.println(user1 + " " + user2 + " " + page + " " + size);
        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        System.out.println(pageable);
        List<MessageEntity> message =  messageRepository.findChatMessages(user1, user2, pageable).getContent();
        System.out.println(message);
        return message;
    }

    public List<UserEntity> getChatParticipants(Long user1) {
        return messageRepository.findChatParticipants(user1);
    }

    public MessageEntity sendMessage(int senderUsername, int receiverUsername, String content) {
        UserEntity sender = null;
        if ((sender = userRepository.findUserEntityById((long) senderUsername)) == null) {
            return null;
        }
        UserEntity receiver = null;
        if ((receiver = userRepository.findUserEntityById((long) receiverUsername)) == null) {
            return null;
        }

        MessageEntity message = new MessageEntity();
        message.setSender(sender);
        message.setRecipient(receiver);
        message.setContent(content);
        message.setTimestamp(LocalDateTime.now());
        return messageRepository.save(message);
    }

    public MessageEntity sendGroupMessage(Long senderId, Long groupId, String content) {
        UserEntity sender = userRepository.findById(senderId).orElseThrow(() -> new RuntimeException("Sender not found"));
        ChatGroup group = chatGroupRepository.findByid(groupId);

        System.out.println("sender" + sender);

        System.out.println("group: " + group.getId());

        MessageEntity message = new MessageEntity();
        message.setSender(sender);
        message.setChatGroup(group);
        message.setRecipient(null);
        message.setContent(content);
        message.setTimestamp(LocalDateTime.now());

        return messageRepository.save(message);
    }

    public List<MessageEntity> getGroupMessages(Long groupId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "timestamp"));
        return messageRepository.findByChatGroup_Id(groupId, pageable);
    }

    public List<ChatGroup> getChatGroups(Long userId) {
        // Benutze das ChatGroupRepository, um alle Gruppen zu finden, in denen der Benutzer Mitglied ist.
        return chatGroupRepository.findByUsers_Id(userId);
    }



    public DirectChat getOrCreateDirectChat(UserEntity user1, UserEntity user2) {
        // Versuche, den Chat in beiden Richtungen zu finden
        DirectChat chatOptional;
        if(directChatRepository.findByUser1IdAndUser2Id(user1.getId(), user2.getId()) == null){
            chatOptional = directChatRepository.findByUser2IdAndUser1Id(user1.getId(), user2.getId());
        }else if (directChatRepository.findByUser2IdAndUser1Id(user1.getId(), user2.getId()) != null){
            chatOptional = directChatRepository.findByUser1IdAndUser2Id(user1.getId(), user2.getId());
        }else {
            return null;
        }

        // Falls der Chat nicht existiert, erstelle einen neuen
        DirectChat newChat = new DirectChat();
        newChat.setUser1(user1);
        newChat.setUser2(user2);
        return directChatRepository.save(newChat);
    }

    public List<DirectChat> getDirectChats(Long userId) {
        // Holen der Direkt-Chats für den Benutzer
        return directChatRepository.findByUser1IdOrUser2Id(userId, userId);
    }

    public DirectChat getDirectChat(Long user1Id, Long user2Id) {
        // Holen des DirektChats zwischen zwei Benutzern
        if(directChatRepository.findByUser1IdAndUser2Id(user1Id, user2Id) != null){
            return directChatRepository.findByUser2IdAndUser1Id(user2Id, user1Id);
        }
        return null;
    }

}
