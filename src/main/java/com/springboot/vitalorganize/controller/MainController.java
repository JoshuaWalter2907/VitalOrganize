package com.springboot.vitalorganize.controller;
import com.springboot.vitalorganize.model.*;
import com.springboot.vitalorganize.repository.UserRepository;
import com.springboot.vitalorganize.service.AuthenticationService;
import com.springboot.vitalorganize.service.PaypalService;
import com.springboot.vitalorganize.service.TwoFactorService;
import com.springboot.vitalorganize.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;


@Controller
@AllArgsConstructor
public class MainController {

    private final UserService userService;
    private final AuthenticationService authenticationService;
    private final UserRepository userRepository;
    private final PaypalService paypalService;
    private final TwoFactorService twoFactorService;



    @RequestMapping("/")
    public String home(
            Model model
    ) {

        authenticationService.getAuthenticatedUsername()
                .ifPresent(username -> model.addAttribute("username", username));

        return "home";
    }

    @GetMapping("/api-docs")
    public String apiDocs(Model model) {
        return "api-docs";
    }

    @GetMapping("/change-theme")
    public String changeTheme(
            HttpServletRequest request
    ) {

        String referer = request.getHeader("Referer");
        if (referer != null) {
            return "redirect:" + referer;
        }

        return "redirect:/";
    }

    @GetMapping("/login")
    public String login(
            Model model
    ) {
        return "LoginPage";
    }


    @PostMapping("/send-2fa-code")
    public String sendTwoFactorCode(
            @RequestParam(name = "email", required = false) String email,
            @RequestParam(required = false) String inputString,
            @RequestParam(required = false) String birthDate,
            @RequestParam(required = false) Boolean isPublic,
            @AuthenticationPrincipal OAuth2User user,
            OAuth2AuthenticationToken auth2AuthenticationToken,
            HttpSession session
    ) {
        String uri = (String) session.getAttribute("uri");
        if ("/profileaddition".equals(uri)) {
            session.setAttribute("email", email);
            session.setAttribute("inputString", inputString);
            session.setAttribute("birthDate", birthDate);
            session.setAttribute("isPublic", isPublic);
        }

        UserEntity userEntity = userService.getCurrentUser(user, auth2AuthenticationToken);

        // Prüfe, ob die E-Mail vorhanden ist, ansonsten nimm die vom Benutzer
        if (userEntity.getProvider().equals("github") && userEntity.getSendtoEmail() != null) {
            email = userEntity.getSendtoEmail();
        }

        // Generiere und sende den Code
        twoFactorService.generateAndSendCode(userEntity, email);

        return "redirect:" + uri + "?fa=true";
    }

    @PostMapping("/verify-2fa")
    public String verifyTwoFactorCode(
            @AuthenticationPrincipal OAuth2User user,
            OAuth2AuthenticationToken auth2AuthenticationToken,
            @RequestParam Map<String, String> digits,
            HttpSession session
    ) {
        String uri = (String) session.getAttribute("uri");

        boolean isVerified = twoFactorService.verifyCode(user, auth2AuthenticationToken, digits, session);

        if (isVerified) {
            if ("/profile-edit".equals(uri)) {
                return "redirect:/profile-edit";
            } else if ("/profileaddition".equals(uri)) {
                return "forward:/profileaddition";
            }
        }

        return "redirect:/error";
    }




}
