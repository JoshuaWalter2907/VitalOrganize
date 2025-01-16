package com.springboot.vitalorganize.service;

import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service-Klasse zur Handhabung von Authentifizierungs-bezogenen Aufgaben bevor der Nutzer eingeloggt ist.
 */
@Service
@AllArgsConstructor
public class AuthenticationService {

    /**
     * Gibt den Benutzernamen des authentifizierten Benutzers zurück, falls der Benutzer authentifiziert ist.
     * @return den Benutzernamen des authentifizierten Benutzers, falls vorhanden, andernfalls Optional.empty()
     */
    public Optional<String> getAuthenticatedUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return Optional.of(authentication.getName());
        }
        return Optional.empty();
    }
}
