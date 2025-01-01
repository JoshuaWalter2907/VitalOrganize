package com.springboot.vitalorganize.service;

import com.springboot.vitalorganize.model.UserEntity;
import com.springboot.vitalorganize.service.repositoryhelper.UserRepositoryService;
import lombok.AllArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
public class MembershipScheduler {

    private final UserRepositoryService userRepositoryService;


    @Scheduled(cron = "0 0 0 * * ?")
    public void checkUserMemberships() {
        List<UserEntity> users = userRepositoryService.findAllUsers();
        LocalDateTime now = LocalDateTime.now();

        for (UserEntity user : users) {
            if(!user.isMember());{
                user.setRole("USER");
            }
        }
    }
}
