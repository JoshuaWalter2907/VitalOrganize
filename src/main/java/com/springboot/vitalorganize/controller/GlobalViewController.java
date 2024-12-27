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
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@AllArgsConstructor
public class GlobalViewController {

    private final UserService userService;


    @ModelAttribute
    public void addGlobalAttributes(@AuthenticationPrincipal OAuth2User user,
                                    OAuth2AuthenticationToken authentication,
                                    Model model,
                                    HttpServletRequest request
    ) {
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