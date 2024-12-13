package com.springboot.vitalorganize.service;

import com.springboot.vitalorganize.model.MessageEntity;
import com.springboot.vitalorganize.model.MessageRepository;
import com.springboot.vitalorganize.model.UserEntity;
import com.springboot.vitalorganize.model.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
public class ChatService {

    private final UserRepository userRepository;

    private final MessageRepository messageRepository;

    public List<MessageEntity> getMessages(Long user1, Long user2, int page, int size) {
        System.out.println(user1 + " " + user2 + " " + page + " " + size);
        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        System.out.println(pageable);
        List<MessageEntity> message =  messageRepository.findChatMessages(user1, user2, pageable).getContent();
        System.out.println(message);
        return message;
    }

    public MessageEntity sendMessage(String senderUsername, String receiverUsername, String content) {
        UserEntity sender = null;
        if (userRepository.findByUsername(senderUsername).isPresent()) {
            sender = userRepository.findByUsername(senderUsername).get();
        }
        UserEntity receiver = null;
        if (userRepository.findByUsername(receiverUsername).isPresent()) {
            receiver = userRepository.findByUsername(receiverUsername).get();
        }

        MessageEntity message = new MessageEntity();
        message.setSender(sender);
        message.setRecipient(receiver);
        message.setContent(content);
        message.setTimestamp(LocalDateTime.now());
        return messageRepository.save(message);
    }

}
