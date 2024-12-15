package com.springboot.vitalorganize.model;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatGroupRepository extends JpaRepository<ChatGroup, Long> {

    List<ChatGroup> findByUsers_Id(Long userId);

    ChatGroup findByid(Long groupId);
}
