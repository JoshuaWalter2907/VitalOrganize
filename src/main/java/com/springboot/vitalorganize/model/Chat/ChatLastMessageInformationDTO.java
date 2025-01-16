package com.springboot.vitalorganize.model.Chat;

import com.springboot.vitalorganize.entity.Chat.DirectChatEntity;
import com.springboot.vitalorganize.entity.Chat.ChatGroupEntity;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ChatLastMessageInformationDTO {
    private DirectChatEntity directChat;
    private ChatGroupEntity groupChat;
    private String lastMessage;
    private LocalDateTime lastMessageTime;

    public ChatLastMessageInformationDTO(DirectChatEntity directChat, String lastMessage, LocalDateTime lastMessageTime) {
        this.directChat = directChat;
        this.lastMessage = lastMessage;
        this.lastMessageTime = lastMessageTime;
    }

    public ChatLastMessageInformationDTO(ChatGroupEntity groupChat, String lastMessage, LocalDateTime lastMessageTime) {
        this.groupChat = groupChat;
        this.lastMessage = lastMessage;
        this.lastMessageTime = lastMessageTime;
    }

    public boolean isGroupChat() {
        return groupChat != null;
    }

    @Override
    public String toString() {
        return "ChatDetail{" +
                "directChat=" + directChat +
                ", groupChat=" + groupChat +
                ", lastMessage='" + lastMessage + '\'' +
                ", lastMessageTime=" + lastMessageTime +
                '}';

    }
}