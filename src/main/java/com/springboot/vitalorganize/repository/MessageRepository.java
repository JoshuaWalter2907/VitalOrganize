package com.springboot.vitalorganize.repository;

import com.springboot.vitalorganize.model.MessageEntity;
import com.springboot.vitalorganize.model.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MessageRepository extends JpaRepository<MessageEntity, Long> {
    @Query("SELECT m FROM MessageEntity m WHERE " +
            "(m.sender.id = :user1 AND m.recipient.id = :user2) OR " +
            "(m.sender.id = :user2 AND m.recipient.id = :user1) " +
            "ORDER BY m.timestamp ASC ")
    Page<MessageEntity> findChatMessages(
            @Param("user1") Long user1,
            @Param("user2") Long user2,
            Pageable pageable);

    @Query("SELECT DISTINCT u FROM UserEntity u " +
            "WHERE u IN (SELECT m.sender FROM MessageEntity m WHERE m.recipient.id = :user) " +
            "OR u IN (SELECT m.recipient FROM MessageEntity m WHERE m.sender.id = :user)")
    List<UserEntity> findChatParticipants(@Param("user") Long user);


    List<MessageEntity> findByChatGroup_Id(Long groupId, Pageable pageable);

    @Query("SELECT m FROM MessageEntity m WHERE m.directChat.id = :chatId ORDER BY m.timestamp DESC")
    List<MessageEntity> findLastMessageForDirectChat(@Param("chatId") Long chatId, Pageable pageable);

    // Für ChatGroup: Holen der letzten Nachricht für eine ChatGroup
    @Query("SELECT m FROM MessageEntity m WHERE m.chatGroup.id = :groupId ORDER BY m.timestamp DESC")
    List<MessageEntity> findLastMessageForChatGroup(@Param("groupId") Long groupId, Pageable pageable);

    void deleteByRecipient_Id(Long id);

    void deleteBySender_Id(Long id);
}
