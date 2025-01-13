package com.springboot.vitalorganize.model;

import com.springboot.vitalorganize.entity.DirectChatEntity;
import com.springboot.vitalorganize.entity.ChatGroupEntity;

import java.time.LocalDateTime;

public class ChatDetail {
    private DirectChatEntity directChat;
    private ChatGroupEntity groupChat;
    private String lastMessage;
    private LocalDateTime lastMessageTime;

    public ChatDetail(DirectChatEntity directChat, String lastMessage, LocalDateTime lastMessageTime) {
        this.directChat = directChat;
        this.lastMessage = lastMessage;
        this.lastMessageTime = lastMessageTime;
    }

    public ChatDetail(ChatGroupEntity groupChat, String lastMessage, LocalDateTime lastMessageTime) {
        this.groupChat = groupChat;
        this.lastMessage = lastMessage;
        this.lastMessageTime = lastMessageTime;
    }

    public DirectChatEntity getDirectChat() {
        return directChat;
    }

    public ChatGroupEntity getGroupChat() {
        return groupChat;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public LocalDateTime getLastMessageTime() {
        return lastMessageTime;
    }

    public boolean isDirectChat() {
        return directChat != null;
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