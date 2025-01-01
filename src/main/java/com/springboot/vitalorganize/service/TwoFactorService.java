package com.springboot.vitalorganize.service;

import com.springboot.vitalorganize.model.UserEntity;
import com.springboot.vitalorganize.repository.UserRepository;
import com.springboot.vitalorganize.service.repositoryhelper.UserRepositoryService;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;

@Service
@AllArgsConstructor
public class TwoFactorService {

    private final SenderService senderService;
    private UserRepositoryService userRepositoryService;
    private UserService userService;

    public void generateAndSendCode(UserEntity userEntity, String email) {
        String code = String.format("%06d", new Random().nextInt(999999));
        userEntity.setTwoFactorCode(code);
        userEntity.setTwoFactorExpiry(LocalDateTime.now().plusMinutes(5));
        userRepositoryService.saveUser(userEntity);

        senderService.sendEmail(email, "Your 2FA Code", "Your code is: " + code);
    }

    public boolean verifyCode(OAuth2User user, OAuth2AuthenticationToken auth2AuthenticationToken,
                              Map<String, String> digits, HttpSession session) {
        String code = String.join("", digits.values());
        UserEntity userEntity = userService.getCurrentUser(user, auth2AuthenticationToken);

        if (userEntity.getTwoFactorCode().equals(code) &&
                userEntity.getTwoFactorExpiry().isAfter(LocalDateTime.now())) {

            userEntity.setTwoFactorCode(null);
            userEntity.setTwoFactorExpiry(null);
            userRepositoryService.saveUser(userEntity);

            session.setAttribute("2fa_verified", true);
            return true;
        }

        return false;
    }
}
