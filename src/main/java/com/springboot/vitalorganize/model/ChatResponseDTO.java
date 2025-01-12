package com.springboot.vitalorganize.model;

import com.springboot.vitalorganize.entity.ChatGroup;
import com.springboot.vitalorganize.entity.DirectChat;
import com.springboot.vitalorganize.entity.MessageEntity;
import com.springboot.vitalorganize.entity.UserEntity;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ChatResponseDTO {
    private String currentUser;
    private Long senderId;
    private List<ChatGroup> chatGroups;
    private List<DirectChat> directChats;
    private List<UserEntity> chatParticipants;
    private List<Object> filteredChatList;
    private List<ChatDetail> chatDetails;
    private ChatGroup selectedGroup;
    private UserEntity selectedUser;
    private DirectChat selectedDirectChat;
    private List<MessageEntity> messages;
    private String otherUserName;
    private String otherUserPicture;
    private Long groupId;
    private Long chatId;
    private Long recipientId;
    private String query;

    @Override
    public String toString() {
        return "ChatResponseDTO{" +
                "currentUser='" + currentUser + '\'' +
                ", senderId=" + senderId +
                ", chatGroups=" + chatGroups +
                ", directChats=" + directChats +
                ", chatParticipants=" + chatParticipants +
                ", filteredChatList=" + filteredChatList +
                ", chatDetails=" + chatDetails +
                ", selectedGroup=" + selectedGroup +
                ", selectedUser=" + selectedUser +
                ", selectedDirectChat=" + selectedDirectChat +
                ", messages=" + messages +
                ", otherUserName='" + otherUserName + '\'' +
                ", otherUserPicture='" + otherUserPicture + '\'' +
                ", groupId=" + groupId +
                ", chatId=" + chatId +
                ", recipientId=" + recipientId +
                ", query='" + query + '\'' +
                '}';
    }

}
