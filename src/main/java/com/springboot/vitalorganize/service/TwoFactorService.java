package com.springboot.vitalorganize.service;

import com.springboot.vitalorganize.entity.Profile_User.UserEntity;
import com.springboot.vitalorganize.model.Profile.FARequestDTO;
import com.springboot.vitalorganize.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
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
    private final UserRepository userRepository;
    private final UserService userService;

    /**
     * Generiert einen neuen Zwei-Faktor-Authentifizierungscode und sendet diesen an die E-Mail des Benutzers
     * Der Code ist für 5 Minuten gültig
     */
    public void generateAndSendCode(FARequestDTO faRequestDTO) {
        UserEntity userEntity = userService.getCurrentUser();
        String email = faRequestDTO.getEmail();

        String code = String.format("%06d", new Random().nextInt(999999));

        userEntity.setTwoFactorCode(code);
        userEntity.setTwoFactorExpiry(LocalDateTime.now().plusMinutes(5));

        userRepository.save(userEntity);

        senderService.sendEmail(email, "Your 2FA Code", "Your code is: " + code);
    }

    /**
     * Überprüft, ob der eingegebene Zwei-Faktor-Authentifizierungscode korrekt ist und innerhalb der Gültigkeitsdauer liegt
     *
     * @param digits Eine Map von Zeichen die den eingegebenen Code repräsentieren
     * @param session Die HTTP-Session des Benutzers, um die erfolgreiche Verifizierung zu speichern
     * @return true, wenn der Code korrekt ist und noch gültig ist
     */
    public boolean verifyCode(List<String> digits, HttpSession session) {
        String code = String.join("", digits);

        UserEntity userEntity = userService.getCurrentUser();

        if (userEntity.getTwoFactorCode().equals(code) &&
                userEntity.getTwoFactorExpiry().isAfter(LocalDateTime.now())) {

            userEntity.setTwoFactorCode(null);
            userEntity.setTwoFactorExpiry(null);
            userRepository.save(userEntity);

            session.setAttribute("2fa_verified", true);
            return true;
        }

        return false;
    }
}

