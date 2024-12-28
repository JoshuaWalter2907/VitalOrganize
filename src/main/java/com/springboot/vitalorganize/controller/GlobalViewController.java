package com.springboot.vitalorganize.controller;

import com.springboot.vitalorganize.model.UserEntity;
import com.springboot.vitalorganize.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;

@ControllerAdvice
@AllArgsConstructor
public class GlobalViewController {

    private final UserService userService;


    @ModelAttribute
    public void addGlobalAttributes(@AuthenticationPrincipal OAuth2User user,
                                    OAuth2AuthenticationToken authentication,
                                    @RequestParam(name = "Color", defaultValue = "black") String color,
                                    Model model,
                                    HttpServletRequest request
    ) {

        String theme = request.getParameter("theme");

        // Wenn der theme-Parameter vorhanden ist, speichere ihn in der Session
        if (theme != null && !theme.isEmpty()) {
            // Setze das Theme in der Session, damit es auch in späteren Anfragen verfügbar ist
            request.getSession().setAttribute("theme", theme);
        }

        // Wenn kein theme-Parameter gesetzt ist, prüfe, ob ein Theme in der Session existiert
        if (theme == null) {
            theme = (String) request.getSession().getAttribute("theme");
        }

        // Falls kein Theme gesetzt ist, verwende das Standard-Theme (z.B. "light")
        if (theme == null) {
            theme = "blue"; // Default-Theme
        }

        // Setze das Theme im Request-Attribut, damit es im HTML verwendet werden kann
        request.setAttribute("theme", theme);

        String username = null;
        if(user != null || authentication != null) {
            UserEntity userEntity = userService.getCurrentUser(user, authentication);

            username = userEntity.getUsername();
            model.addAttribute("loggedInUser", userEntity);
        }


        model.addAttribute("httpServletRequest", request);
        System.out.println(request.getRequestURI());
        if (username != null) {
            model.addAttribute("username", username); // Beispielattribut
        } else {
            model.addAttribute("username", null);
        }

    }



}