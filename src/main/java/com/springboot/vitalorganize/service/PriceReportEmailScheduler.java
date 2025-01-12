package com.springboot.vitalorganize.service;

import com.springboot.vitalorganize.entity.UserEntity;
import com.springboot.vitalorganize.service.repositoryhelper.UserRepositoryService;
import lombok.AllArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class PriceReportEmailScheduler {

    private final IngredientListService ingredientListService;
    private final UserRepositoryService userRepositoryService;


    @Scheduled(cron = "0 0 8 * * MON") // send the email every monday at 8:00
    public void sendWeeklyEmails() {
        List<UserEntity> users = userRepositoryService.findAllUsers();

        for (UserEntity user : users) {
            if (user.isPriceReportsEnabled()) {
                ingredientListService.sendEmailWithPrices(user.getId());
            }
        }
    }
}