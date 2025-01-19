package com.springboot.vitalorganize.service;

import com.springboot.vitalorganize.entity.Profile_User.UserEntity;
import com.springboot.vitalorganize.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Dieser Service ist zuständig für das wiederholte Senden einer Email an einen Nutzer
 * Diese Klasse bietet eine Methode für das wöchentliche Senden der Email
 */

@Service
@AllArgsConstructor
public class PriceReportEmailScheduler {

    private final PriceReportEmailService priceReportEmailService;
    private final UserRepository userRepository;

    /**
     * Sendet jeden Montag eine Email an alle Nutzer, die diese Option aktiviert haben
     */
    @Scheduled(cron = "0 0 8 * * MON") // send the email every monday at 8:00
    public void sendWeeklyEmails() {
        List<UserEntity> users = userRepository.findAll();

        for (UserEntity user : users) {
            if (user.isPriceReportsEnabled()) {
                priceReportEmailService.sendEmailWithPrices(user.getId());
            }
        }
    }
}