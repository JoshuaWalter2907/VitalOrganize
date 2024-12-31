package com.springboot.vitalorganize.component;

import com.springboot.vitalorganize.model.UserEntity;
import com.springboot.vitalorganize.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@AllArgsConstructor
public class UsernameInterceptor implements HandlerInterceptor {

    private final UserService userService;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        if(request.getRequestURI().startsWith("/api")) {
            return true;
        }

        // Überprüfe, ob die Authentifizierung im SecurityContext vorhanden ist
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Überprüfe, ob der Authentication-Token ein OAuth2AuthenticationToken ist
        if (authentication instanceof OAuth2AuthenticationToken) {
            OAuth2AuthenticationToken oauth2Authentication = (OAuth2AuthenticationToken) authentication;

            if (oauth2Authentication.isAuthenticated()) {
                OAuth2User user = oauth2Authentication.getPrincipal();

                // Hole den aktuellen Benutzer aus dem Service
                UserEntity userEntity = userService.getCurrentUser(user, oauth2Authentication);

                // Überprüfe, ob der Benutzername gesetzt ist
                String username = userEntity.getUsername();
                if (username == null || username.isEmpty()) {
                    // Wenn der Benutzername fehlt, leite zur Profil-Setzseite weiter
                    response.sendRedirect("/profileaddition");
                    return false; // Abbrechen der Anfrage
                }
            } else {
                // Wenn der Benutzer nicht authentifiziert ist, leite auf die Login-Seite weiter
                response.sendRedirect("/login");
                return false;
            }
        } else {
            // Falls der Authentication-Typ nicht OAuth2AuthenticationToken ist
            response.sendRedirect("/login");
            return false;
        }

        // Wenn alles gut ist, fahre mit der Anfrage fort
        return true;
    }

}
