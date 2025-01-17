package com.springboot.vitalorganize.repository;

import com.springboot.vitalorganize.entity.Profile_User.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
    UserEntity findByEmailAndProvider(String email, String provider);
    UserEntity findUserEntityById(Long user2);

    List<UserEntity> findAllByisPublic(boolean isPublic);

    List<UserEntity> findByUsernameContainingIgnoreCase(String query);

    UserEntity findByToken(String token);
}
