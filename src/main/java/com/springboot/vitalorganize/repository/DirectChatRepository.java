package com.springboot.vitalorganize.repository;

import com.springboot.vitalorganize.entity.Chat.DirectChatEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DirectChatRepository extends JpaRepository<DirectChatEntity, Long> {
    DirectChatEntity findByUser1IdAndUser2Id(Long id, Long id1);

    DirectChatEntity findByUser2IdAndUser1Id(Long id, Long id1);

    List<DirectChatEntity> findByUser1IdOrUser2Id(Long userId, Long userId1);
}
