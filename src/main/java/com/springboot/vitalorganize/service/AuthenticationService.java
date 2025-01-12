package com.springboot.vitalorganize.service;

import com.springboot.vitalorganize.entity.UserEntity;
import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service-Klasse zur Handhabung von Authentifizierungs-bezogenen Aufgaben.
 * Diese Klasse bietet Methoden zum Abrufen des aktuell authentifizierten Benutzers und des Benutzernamens.
 */
@Service
@AllArgsConstructor
public class AuthenticationService {

    private final UserService userService;

    /**
     * Gibt den aktuell authentifizierten Benutzer zurück.
     *
     * @param user der OAuth2 authentifizierte Benutzer
     * @param authentication das OAuth2-Authentifizierungstoken
     * @return der aktuelle Benutzer, wenn authentifiziert, andernfalls null
     */
    public UserEntity getCurrentUser(@AuthenticationPrincipal OAuth2User user, OAuth2AuthenticationToken authentication) {
        // Überprüft, ob ein authentifizierter Benutzer oder ein Token vorhanden ist
        if (user != null || authentication != null) {
            // Gibt den aktuellen Benutzer aus dem UserService zurück
            return userService.getCurrentUser();
        }
        // Gibt null zurück, wenn kein authentifizierter Benutzer vorhanden ist
        return null;
    }

    /**
     * Gibt den Benutzernamen des authentifizierten Benutzers zurück, falls der Benutzer authentifiziert ist.
     *
     * @return den Benutzernamen des authentifizierten Benutzers, falls vorhanden, andernfalls Optional.empty()
     */
    public Optional<String> getAuthenticatedUsername() {
        // Holt die aktuelle Authentifizierung aus dem SecurityContext
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // Überprüft, ob der Benutzer authentifiziert ist
        if (authentication != null && authentication.isAuthenticated()) {
            // Gibt den Benutzernamen des authentifizierten Benutzers zurück
            return Optional.of(authentication.getName());
        }
        // Gibt Optional.empty zurück, wenn der Benutzer nicht authentifiziert ist
        return Optional.empty();
    }
}
