package com.springboot.vitalorganize.service;

import com.springboot.vitalorganize.model.*;
import lombok.AllArgsConstructor;
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
        ChatGroup group = chatGroupRepository.findById(groupId).orElseThrow(() -> new RuntimeException("Group not found"));

        MessageEntity message = new MessageEntity();
        message.setSender(sender);
        message.setChatGroup(group);
        message.setContent(content);
        message.setTimestamp(LocalDateTime.now());

        return messageRepository.save(message);
    }

    public List<MessageEntity> getGroupMessages(Long groupId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        return messageRepository.findByChatGroup_Id(groupId, pageable);
    }

    public List<ChatGroup> getChatGroups(Long userId) {
        // Benutze das ChatGroupRepository, um alle Gruppen zu finden, in denen der Benutzer Mitglied ist.
        return chatGroupRepository.findByUsers_Id(userId);
    }

}
