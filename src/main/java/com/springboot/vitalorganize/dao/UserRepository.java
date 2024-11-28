package com.springboot.vitalorganize.dao;

import com.springboot.vitalorganize.model.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
    UserEntity findByUsernameAndProvider(String username, String provider);
}
