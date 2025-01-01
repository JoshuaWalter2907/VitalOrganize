package com.springboot.vitalorganize.service.repositoryhelper;

import com.springboot.vitalorganize.model.ChatGroup;
import com.springboot.vitalorganize.model.DirectChat;
import com.springboot.vitalorganize.model.UserEntity;
import com.springboot.vitalorganize.repository.ChatGroupRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class ChatGroupRepositoryService {

    private final ChatGroupRepository chatGroupRepository;

    public List<ChatGroup> findChatGroups(Long userId) {
        return chatGroupRepository.findByUsers_Id(userId);
    }

    public Optional<ChatGroup> findByUsersInAndName(List<UserEntity> users, String chatName) {
        return chatGroupRepository.findByUsersInAndName(users, chatName);
    }

    public void saveChatGroup(ChatGroup chatGroup) {
        chatGroupRepository.save(chatGroup);
    }

    public Optional<ChatGroup> findById(Long chatId){
        return chatGroupRepository.findById(chatId);
    }

    public void deleteChatGroup(ChatGroup chat) {
        chatGroupRepository.delete(chat);
    }

    public List<ChatGroup> findChatGroupsContaining(String query) {
        return chatGroupRepository.findByNameContaining(query);
    }

    public List<ChatGroup> findAllByUserId(Long id) {
        return chatGroupRepository.findAllByUserId(id);
    }

    public void deleteAllByIdIn(List<Long> chatGroupIds) {
        chatGroupRepository.deleteAllByIdIn(chatGroupIds);
    }
}
