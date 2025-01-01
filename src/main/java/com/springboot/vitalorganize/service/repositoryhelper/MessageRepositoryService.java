package com.springboot.vitalorganize.service.repositoryhelper;

import com.springboot.vitalorganize.component.PaginationHelper;
import com.springboot.vitalorganize.model.MessageEntity;
import com.springboot.vitalorganize.model.UserEntity;
import com.springboot.vitalorganize.repository.MessageRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class MessageRepositoryService {

    private final MessageRepository messageRepository;
    private final PaginationHelper paginationHelper;


    public List<MessageEntity> ChatMessagesBetweenUsers(Long user1Id, Long user2Id, int page, int size){
        Pageable pageable = paginationHelper.createPageable(page, size, "timestamp", Sort.Direction.DESC);
        return messageRepository.findChatMessages(user1Id, user2Id, pageable).getContent();
    }

    public List<UserEntity> ChatParticipants(Long userId){
        return messageRepository.findChatParticipants(userId);
    }

    public MessageEntity saveMessage(MessageEntity messageEntity){
        return messageRepository.save(messageEntity);
    }

    public List<MessageEntity> findChatParticipants(Long groupId, int page, int size){
        Pageable pageable = paginationHelper.createPageable(page, size, "timestamp", Sort.Direction.ASC);
        return messageRepository.findByChatGroup_Id(groupId, pageable);
    }

    public MessageEntity getLastDirectChatMessage(Long chatId) {
        Pageable pageable = paginationHelper.createSingleItemPageable("timestamp", Sort.Direction.DESC);
        List<MessageEntity> messages = messageRepository.findLastMessageForDirectChat(chatId, pageable);
        return paginationHelper.getFirstElement(messages);
    }

    public MessageEntity getLastGroupChatMessage(Long groupId) {
        Pageable pageable = paginationHelper.createSingleItemPageable("timestamp", Sort.Direction.DESC);
        List<MessageEntity> messages = messageRepository.findLastMessageForChatGroup(groupId, pageable);
        return paginationHelper.getFirstElement(messages);
    }

    public void deleteByRecipient_Id(Long id) {
        messageRepository.deleteByRecipient_Id(id);
    }

    public void deleteBySender_Id(Long id) {
        messageRepository.deleteBySender_Id(id);
    }
}
