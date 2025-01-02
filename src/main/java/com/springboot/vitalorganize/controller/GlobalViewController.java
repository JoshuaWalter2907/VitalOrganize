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

@ControllerAdvice
@AllArgsConstructor
public class GlobalViewController {

    private final ThemeService themeService;
    private final AuthenticationService authenticationService;

    @ModelAttribute
    public void addGlobalAttributes(@AuthenticationPrincipal OAuth2User user,
                                    OAuth2AuthenticationToken authentication,
                                    HttpServletRequest request,
                                    Model model) {

        // Theme aus dem ThemeService abrufen
        String theme = themeService.getTheme(request);
        request.setAttribute("theme", theme);

        UserEntity userEntity = authenticationService.getCurrentUser(user, authentication);

        if (userEntity != null) {
            model.addAttribute("loggedInUser", userEntity);
            model.addAttribute("username", userEntity.getUsername());
        } else {
            model.addAttribute("username", null);
        }

        model.addAttribute("httpServletRequest", request);
    }

}