package com.springboot.vitalorganize.model.Chat;

import com.springboot.vitalorganize.entity.Chat.ChatGroupEntity;
import com.springboot.vitalorganize.entity.Chat.DirectChatEntity;
import com.springboot.vitalorganize.entity.Chat.MessageEntity;
import com.springboot.vitalorganize.entity.Profile_User.UserEntity;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ChatResponseDTO {
    private String currentUser;
    private Long senderId;
    private List<ChatGroupEntity> chatGroups;
    private List<DirectChatEntity> directChats;
    private List<UserEntity> chatParticipants;
    private List<Object> filteredChatList;
    private List<ChatLastMessageInformationDTO> chatDetails;
    private ChatGroupEntity selectedGroup;
    private UserEntity selectedUser;
    private DirectChatEntity selectedDirectChat;
    private List<MessageEntity> messages;
    private String otherUserName;
    private String otherUserPicture;
    private Long groupId;
    private Long chatId;
    private Long recipientId;
    private String query;
    private int page;
    private Long totalMessages;

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
