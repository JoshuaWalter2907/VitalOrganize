package com.springboot.vitalorganize.service;

import com.springboot.vitalorganize.model.UserEntity;
import com.springboot.vitalorganize.service.repositoryhelper.UserRepositoryService;
import lombok.AllArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Dieser Service überwacht die Mitgliedschaften der Benutzer und stellt sicher, dass Benutzer, die nicht Mitglied sind,
 * in eine Standardrolle gesetzt werden. Der Service wird täglich um Mitternacht ausgeführt.
 */
@Service
@AllArgsConstructor
public class MembershipScheduler {

    private final UserRepositoryService userRepositoryService;

    /**
     * Geplante Aufgabe, die täglich um Mitternacht ausgeführt wird.
     * Überprüft alle Benutzer und setzt deren Rolle auf "USER", falls sie kein Mitglied sind.
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void checkUserMemberships() {
        List<UserEntity> users = userRepositoryService.findAllUsers();
        LocalDateTime now = LocalDateTime.now();

        for (UserEntity user : users) {
            // Überprüfen, ob der Benutzer kein Mitglied ist
            if (!user.isMember()) {
                user.setRole("USER"); // Setze die Rolle des Benutzers auf "USER"
            }
        }
    }
}
