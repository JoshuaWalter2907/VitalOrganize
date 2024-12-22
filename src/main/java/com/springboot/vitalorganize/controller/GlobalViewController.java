package com.springboot.vitalorganize.controller;

import com.springboot.vitalorganize.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalViewController {

    @Autowired
    UserService userService;

    @ModelAttribute
    public void addGlobalAttributes(@AuthenticationPrincipal OAuth2User user, Model model) {
        if (user != null) {
            model.addAttribute("username", user.getAttribute("name")); // Beispielattribut
        } else {
            model.addAttribute("username", null);
        }
    }

    @ModelAttribute
    public void addThemeAttribute(HttpSession session, Model model) {
        String theme = (String) session.getAttribute("theme");

        // default value if there is no session yet
        if (theme == null) {
            theme = "light";
            session.setAttribute("theme", theme);
        }

        // gets the css file from the session-attribute "theme"
        model.addAttribute("themeCss", userService.getThemeCss(theme));
    }

    @ModelAttribute
    public void addLanguageAttribute(HttpSession session, Model model) {
        String lang = (String) session.getAttribute("lang");

        // default value if there is no session yet
        if (lang == null) {
            lang = "en";
            session.setAttribute("lang", lang);
        }

        model.addAttribute("lang", lang);
    }
}
