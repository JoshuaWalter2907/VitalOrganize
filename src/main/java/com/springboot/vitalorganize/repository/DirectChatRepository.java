package com.springboot.vitalorganize.repository;

import com.springboot.vitalorganize.model.DirectChat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DirectChatRepository extends JpaRepository<DirectChat, Long> {
    DirectChat findByUser1IdAndUser2Id(Long id, Long id1);

    DirectChat findByUser2IdAndUser1Id(Long id, Long id1);

    List<DirectChat> findByUser1IdOrUser2Id(Long userId, Long userId1);
}
