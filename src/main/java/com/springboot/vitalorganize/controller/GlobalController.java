package com.springboot.vitalorganize.controller;

import com.springboot.vitalorganize.entity.UserEntity;
import com.springboot.vitalorganize.service.AuthenticationService;
import com.springboot.vitalorganize.service.ThemeService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Eine globale Controller-Klasse, die globale Attribute für Views bereitstellt.
 */
@ControllerAdvice
@AllArgsConstructor
public class GlobalController {

    private final ThemeService themeService;
    private final AuthenticationService authenticationService;


    /**
     * Endpoint zum bereitstellen aller globalen Attribute
     * @param user Der eingeloggte User
     * @param authentication Authentifizierung des eingeloggten Users
     * @param request ServletRequest um die aktuelle URL zu erhalten
     * @param model Zur Übergabe von Paramtern an das Frontend
     */
    @ModelAttribute
    public void addGlobalAttributes(@AuthenticationPrincipal OAuth2User user,
                                    OAuth2AuthenticationToken authentication,
                                    HttpServletRequest request,
                                    Model model
    ) {

        String theme = themeService.getTheme(request);
        request.setAttribute("theme", theme);

        String lang = "en";
        if(request.getParameter("lang") != null) {
            lang = request.getParameter("lang");
        };
        model.addAttribute("lang", lang);

        // Aktuell eingeloggten Benutzer über AuthenticationService abrufen
        UserEntity userEntity = authenticationService.getCurrentUser(user, authentication);

        // Benutzerbezogene Attribute zum Modell hinzufügen, falls Benutzer existiert
        if (userEntity != null) {
            model.addAttribute("loggedInUser", userEntity);
            model.addAttribute("username", userEntity.getUsername());
        } else {
            model.addAttribute("username", null);
        }

        // HTTP-Servlet-Anfrage als Attribut hinzufügen
        model.addAttribute("httpServletRequest", request);
    }

}