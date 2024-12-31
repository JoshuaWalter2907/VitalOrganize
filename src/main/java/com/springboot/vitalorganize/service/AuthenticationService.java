package com.springboot.vitalorganize.service;

import com.springboot.vitalorganize.model.UserEntity;
import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@AllArgsConstructor
public class AuthenticationService {

    private final UserService userService;


    public UserEntity getCurrentUser(@AuthenticationPrincipal OAuth2User user, OAuth2AuthenticationToken authentication) {
        if (user != null || authentication != null) {
            return userService.getCurrentUser(user, authentication);
        }
        return null;
    }

    public Optional<String> getAuthenticatedUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return Optional.of(authentication.getName()); // Benutzername des authentifizierten Benutzers
        }
        return Optional.empty(); // Kein authentifizierter Benutzer
    }
}
