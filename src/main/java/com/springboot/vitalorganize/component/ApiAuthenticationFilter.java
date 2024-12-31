package com.springboot.vitalorganize.component;

import com.springboot.vitalorganize.model.UserEntity;
import com.springboot.vitalorganize.repository.UserRepository;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.http.HttpStatus;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

@Component
public class ApiAuthenticationFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;

    public ApiAuthenticationFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String requestURI = request.getRequestURI();

        // Filter nur für /api/ Routen
        if (requestURI.startsWith("/api/")) {
            String authHeader = request.getHeader("Authorization");

            // Header-Validierung
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                handleUnauthorized(response, "Unauthorized: Missing or invalid token");
                return;
            }

            String token = authHeader.substring(7); // Extrahiere den Token

            // Benutzer basierend auf Token abrufen
            UserEntity userEntity = userRepository.findByToken(token);
            if (userEntity == null) {
                handleUnauthorized(response, "Unauthorized: Invalid token");
                return;
            }

            // Authentifizierung im SecurityContext setzen
            Authentication auth = new UsernamePasswordAuthenticationToken(
                    userEntity,
                    null,
                    Collections.emptyList() // Rollen oder Berechtigungen hinzufügen, falls erforderlich
            );
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        // Anfrage weiterleiten
        filterChain.doFilter(request, response);
    }

    private void handleUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json");
        response.getWriter().write("{\"error\": \"" + message + "\"}");
    }
}
