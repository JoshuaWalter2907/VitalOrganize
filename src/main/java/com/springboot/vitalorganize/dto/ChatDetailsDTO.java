package com.springboot.vitalorganize.dto;

import com.springboot.vitalorganize.model.ChatGroup;
import com.springboot.vitalorganize.model.DirectChat;
import com.springboot.vitalorganize.model.MessageEntity;
import com.springboot.vitalorganize.model.UserEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class ChatDetailsDTO {
    private ChatGroup selectedGroup;
    private UserEntity selectedUser;
    private DirectChat selectedDirectChat;
    private List<MessageEntity> messages;
    private String otherUserName;
    private String otherUserPicture;
    private Long groupId;
    private Long chatId;
    private Long recipientId;

    // Konstruktor, Getter und Setter
}
