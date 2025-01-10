package com.springboot.vitalorganize.controller;

import com.springboot.vitalorganize.model.UserEntity;
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
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Eine globale Controller-Klasse, die globale Attribute für Views bereitstellt.
 *
 * Diese Klasse verwendet die @ControllerAdvice-Annotation, um globale Kontexte
 * für alle Controller bereitzustellen. Dadurch können häufig benötigte Attribute
 * zentral verwaltet und an alle Views übergeben werden.
 */
@ControllerAdvice
@AllArgsConstructor
public class GlobalViewController {

    private final ThemeService themeService;
    private final AuthenticationService authenticationService;

    /**
     * Fügt globale Attribute zu allen Views hinzu.
     *
     * @param user der authentifizierte OAuth2-Benutzer (optional)
     * @param authentication das Authentifizierungs-Token für den OAuth2-Benutzer (optional)
     * @param request die aktuelle HTTP-Anfrage
     * @param model das UI-Modell, in das Attribute eingefügt werden
     */
    @ModelAttribute
    public void addGlobalAttributes(@AuthenticationPrincipal OAuth2User user,
                                    OAuth2AuthenticationToken authentication,
                                    HttpServletRequest request,
                                    Model model
    ) {

        // Aktuelles Theme aus dem ThemeService abrufen und zur Anfrage hinzufügen
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

        // HTTP-Servlet-Anfrage als Attribut hinzufügen (z. B. für zusätzliche Metadaten)
        model.addAttribute("httpServletRequest", request);
    }

}