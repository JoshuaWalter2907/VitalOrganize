package com.springboot.vitalorganize.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatGroupRepository extends JpaRepository<ChatGroup, Long> {

    List<ChatGroup> findByUsers_Id(Long userId);
    Optional<ChatGroup> findByUsersInAndName(List<UserEntity> users, String chatName);

    List<ChatGroup> findByNameContaining(String query);

    @Query("SELECT c FROM ChatGroup c JOIN c.users u WHERE u.id = :userId")
    List<ChatGroup> findAllByUserId(@Param("userId") Long userId);

    void deleteAllByIdIn(List<Long> chatGroupIds);
}
