package com.springboot.vitalorganize.model;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
    UserEntity findByEmailAndProvider(String email, String provider);
    UserEntity findUserEntityById(Long user2);
    boolean existsByUsername(String username);

    List<UserEntity> findAllByisPublic(boolean isPublic);

    List<UserEntity> findByUsernameContainingIgnoreCase(String query);
}
