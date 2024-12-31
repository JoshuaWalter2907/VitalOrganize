package com.springboot.vitalorganize.service.repositoryhelper;

import com.springboot.vitalorganize.model.UserEntity;
import com.springboot.vitalorganize.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class UserRepositoryService {

    private final UserRepository userRepository;

    public List<UserEntity> findUsersByIds(List<Long> userIds) {
        return userRepository.findAllById(userIds);
    }

    public void saveUser(UserEntity userEntity) {
        userRepository.save(userEntity);
    }

    public UserEntity findUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));
    }

    public List<UserEntity> findAllUsers() {
        return userRepository.findAll();
    }

}
