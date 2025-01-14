package com.springboot.vitalorganize.repository;

import com.springboot.vitalorganize.entity.Chat.MessageEntity;
import com.springboot.vitalorganize.entity.Profile_User.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MessageRepository extends JpaRepository<MessageEntity, Long> {
    Page<MessageEntity> findBySenderIdAndRecipientIdOrRecipientIdAndSenderId(
            Long senderId, Long recipientId, Long recipientId2, Long senderId2, Pageable pageable);

    List<MessageEntity> findBySenderIdOrRecipientId(Long senderId, Long recipientId);


    Page<MessageEntity> findByChatGroup_Id(Long groupId, Pageable pageable);


    void deleteByRecipient_Id(Long id);

    void deleteBySender_Id(Long id);

    Page<MessageEntity> findByDirectChatId(Long chatId, Pageable pageable);
    Page<MessageEntity> findByChatGroupId(Long groupId, Pageable pageable);
}
