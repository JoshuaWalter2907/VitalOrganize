package com.springboot.vitalorganize.component;

import com.springboot.vitalorganize.model.UserEntity;
import com.springboot.vitalorganize.model.UserRepository;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.GenericFilterBean;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@WebFilter("/api/*") // Filter für alle /api-Requests
public class ApiAuthenticationFilter extends OncePerRequestFilter {

    private final UserRepository userRepository; // UserRepository für die Token-Überprüfung

    public ApiAuthenticationFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        // Überprüfen, ob die Anfrage auf /api/ zugreift
        String requestURI = request.getRequestURI();
        if (requestURI.startsWith("/api/")) {
            // Überprüfe, ob der Authorization-Header vorhanden ist
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.getWriter().write("Unauthorized: Missing or invalid token");
                return; // Verhindere, dass die Anfrage weitergeht
            }

            // Extrahiere den Token aus dem Header
            String token = authHeader.substring(7);

            // Überprüfe den Token in der Datenbank
            UserEntity userEntity = userRepository.findByToken(token);
            if (userEntity == null) {
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.getWriter().write("Unauthorized: Invalid token");
                return;
            }
        }

        // Führe die Anfrage fort
        filterChain.doFilter(request, response);
    }

}
