package com.springboot.vitalorganize.service;

import com.springboot.vitalorganize.entity.UserEntity;import com.springboot.vitalorganize.service.repositoryhelper.UserRepositoryService;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;

/**
 * Service-Klasse zur Handhabung der Zwei-Faktor-Authentifizierung (2FA) für Benutzer.
 * Diese Klasse bietet Methoden zum Generieren eines 2FA-Codes, dem Senden des Codes an den Benutzer
 * und der Verifizierung des Codes, um die Sicherheit der Anwendung zu erhöhen.
 */
@Service
@AllArgsConstructor
public class TwoFactorService {

    // Abhängigkeiten: Services, die für das Senden von E-Mails und das Verwalten von Benutzerdaten benötigt werden
    private final SenderService senderService;
    private final UserRepositoryService userRepositoryService;
    private final UserService userService;

    /**
     * Generiert einen neuen Zwei-Faktor-Authentifizierungscode (2FA) und sendet diesen an die E-Mail des Benutzers.
     * Der Code ist für 5 Minuten gültig.
     */
    public void generateAndSendCode() {
        UserEntity userEntity = userService.getCurrentUser();
        String email = userService.getEmail(userEntity);
        // Generiert einen zufälligen 6-stelligen Code für die Zwei-Faktor-Authentifizierung
        String code = String.format("%06d", new Random().nextInt(999999));

        // Setzt den 2FA-Code und die Ablaufzeit des Codes (5 Minuten ab jetzt)
        userEntity.setTwoFactorCode(code);
        userEntity.setTwoFactorExpiry(LocalDateTime.now().plusMinutes(5));

        // Speichert den Benutzer mit dem neuen 2FA-Code und Ablaufzeit in der Datenbank
        userRepositoryService.saveUser(userEntity);

        // Sendet den 2FA-Code an die angegebene E-Mail-Adresse des Benutzers
        senderService.sendEmail(email, "Your 2FA Code", "Your code is: " + code);
    }

    /**
     * Überprüft, ob der eingegebene Zwei-Faktor-Authentifizierungscode korrekt ist und innerhalb der Gültigkeitsdauer liegt.
     *
     * @param digits Eine Map von Zeichen (z.B. "1", "2", etc.), die den eingegebenen Code repräsentieren
     * @param session Die HTTP-Session des Benutzers, um die erfolgreiche Verifizierung zu speichern
     * @return true, wenn der Code korrekt ist und noch gültig ist; andernfalls false
     */
    public boolean verifyCode(Map<String, String> digits, HttpSession session) {
        // Verbindet die einzelnen Ziffern des Codes zu einer vollständigen Zahl
        String code = String.join("", digits.values());

        // Holt den aktuellen Benutzer basierend auf dem OAuth2-Token
        UserEntity userEntity = userService.getCurrentUser();

        // Überprüft, ob der eingegebene Code mit dem gespeicherten Code übereinstimmt und ob er noch gültig ist
        if (userEntity.getTwoFactorCode().equals(code) &&
                userEntity.getTwoFactorExpiry().isAfter(LocalDateTime.now())) {

            // Löscht den 2FA-Code und die Ablaufzeit nach erfolgreicher Verifizierung
            userEntity.setTwoFactorCode(null);
            userEntity.setTwoFactorExpiry(null);
            userRepositoryService.saveUser(userEntity);

            // Speichert die erfolgreiche 2FA-Verifizierung in der Session
            session.setAttribute("2fa_verified", true);
            return true;  // Erfolgreiche Verifizierung
        }

        return false;  // Fehlerhafte oder abgelaufene Code-Verifizierung
    }
}

