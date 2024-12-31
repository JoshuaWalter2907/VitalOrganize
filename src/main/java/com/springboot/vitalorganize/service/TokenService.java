package com.springboot.vitalorganize.service;

import org.springframework.stereotype.Service;

@Service
public class TokenService {
    public boolean validateToken(String token) {
        // Implementiere die Validierungslogik
        return token != null && !token.isEmpty();
    }
}
