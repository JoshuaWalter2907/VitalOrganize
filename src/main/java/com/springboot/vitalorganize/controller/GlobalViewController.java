package com.springboot.vitalorganize.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalViewController {

    @ModelAttribute
    public void addGlobalAttributes(@AuthenticationPrincipal OAuth2User user, Model model) {
        if (user != null) {
            model.addAttribute("username", user.getAttribute("name")); // Beispielattribut
        } else {
            model.addAttribute("username", null);
        }
    }

}
