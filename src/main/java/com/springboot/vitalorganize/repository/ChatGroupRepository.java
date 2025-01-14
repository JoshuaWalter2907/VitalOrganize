package com.springboot.vitalorganize.repository;

import com.springboot.vitalorganize.entity.Chat.ChatGroupEntity;
import com.springboot.vitalorganize.entity.Profile_User.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatGroupRepository extends JpaRepository<ChatGroupEntity, Long> {

    List<ChatGroupEntity> findByUsers_Id(Long userId);
    Optional<ChatGroupEntity> findByUsersInAndName(List<UserEntity> users, String chatName);

    List<ChatGroupEntity> findByNameContaining(String query);

    @Query("SELECT c FROM ChatGroupEntity c JOIN c.users u WHERE u.id = :userId")
    List<ChatGroupEntity> findAllByUserId(@Param("userId") Long userId);

    void deleteAllByIdIn(List<Long> chatGroupIds);
}
