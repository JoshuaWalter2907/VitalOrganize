package com.springboot.vitalorganize.dto;

import com.springboot.vitalorganize.model.DirectChat;
import com.springboot.vitalorganize.model.ChatGroup;

import java.time.LocalDateTime;

public class ChatDetail {
    private DirectChat directChat;
    private ChatGroup groupChat;
    private String lastMessage;
    private LocalDateTime lastMessageTime;

    public ChatDetail(DirectChat directChat, String lastMessage, LocalDateTime lastMessageTime) {
        this.directChat = directChat;
        this.lastMessage = lastMessage;
        this.lastMessageTime = lastMessageTime;
    }

    public ChatDetail(ChatGroup groupChat, String lastMessage, LocalDateTime lastMessageTime) {
        this.groupChat = groupChat;
        this.lastMessage = lastMessage;
        this.lastMessageTime = lastMessageTime;
    }

    public DirectChat getDirectChat() {
        return directChat;
    }

    public ChatGroup getGroupChat() {
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