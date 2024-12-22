package com.springboot.vitalorganize.controller;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import com.springboot.vitalorganize.dto.ProfileAdditionData;
import com.springboot.vitalorganize.model.*;
import com.springboot.vitalorganize.service.AuthenticationService;
import com.springboot.vitalorganize.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;

import java.io.OutputStream;
import java.time.LocalDateTime;
import java.util.Random;


@Controller
@AllArgsConstructor
public class MainController {

    private final UserService userService;
    private final AuthenticationService authenticationService;
    private final UserRepository userRepository;


    @RequestMapping("/")
    public String home(
            @RequestParam(value = "theme", defaultValue = "light") String theme,
            @RequestParam(value = "lang", defaultValue = "en") String lang,
            Model model
    ) {

        authenticationService.getAuthenticatedUsername()
                .ifPresent(username -> model.addAttribute("username", username));

        model.addAttribute("themeCss", userService.getThemeCss(theme));
        model.addAttribute("lang", lang);

        return "home";
    }

    @GetMapping("/login")
    public String login(@RequestParam(value = "theme", defaultValue = "light") String theme,
                        @RequestParam(value = "lang", defaultValue = "en") String lang,
                        Model model) {

        model.addAttribute("themeCss", userService.getThemeCss(theme));
        model.addAttribute("lang", lang);
        return "LoginPage";
    }

    @GetMapping("/profile")
    public String profile(@RequestParam(value = "theme", defaultValue = "light") String theme,
                          @RequestParam(value = "lang", defaultValue = "en") String lang,
                          @RequestParam(value ="profileId", required = false) Long profileId,
                          @AuthenticationPrincipal OAuth2User user,
                          OAuth2AuthenticationToken authentication,
                          Model model) {

        UserEntity profileData;
        // Benutzer- und Profilinformationen abrufen
        if(profileId == null) {
            profileData = userService.getProfileData(user, authentication);

            // Model mit den benötigten Daten befüllen
            model.addAttribute("profile", profileData);
        }else {
            profileData = userService.getUserById(profileId);

            model.addAttribute("themeCss", userService.getThemeCss(theme));
            model.addAttribute("lang", lang);

        }
        model.addAttribute("profile", profileData);

        return "private-profile"; // Gibt die View "private-profile.html" zurück
    }

    @GetMapping("/profileaddition")
    public String profileAdditionGet(@RequestParam(value = "theme", defaultValue = "light") String theme,
                                     Model model,
                                     @AuthenticationPrincipal OAuth2User user,
                                     OAuth2AuthenticationToken authentication) {

        // Benutzerinformationen und bestehendes Profil abrufen
        ProfileAdditionData profileData = userService.getProfileAdditionData(user, authentication);

        if (profileData.isProfileComplete()) {
            return "redirect:/profile"; // Weiterleitung, wenn das Profil vollständig ist
        }

        // Daten für die View vorbereiten
        model.addAttribute("user", profileData.getUserEntity());
        model.addAttribute("birthDate", profileData.getBirthDate());
        model.addAttribute("themeCss", userService.getThemeCss(theme));

        return "Profile Additions"; // Thymeleaf-Template für die Profilerweiterung
    }

    @PostMapping("/profileaddition")
    public String profileAddition(@RequestParam("inputString") String inputString,
                                  @RequestParam(name = "isPublic", required = false) boolean status,
                                  @RequestParam("birthDate") String birthDate,
                                  @AuthenticationPrincipal OAuth2User user,
                                  OAuth2AuthenticationToken authentication,
                                  Model model) {

        // Profilaktualisierung durchführen
        boolean usernameExists = userService.updateUserProfile(user, authentication, inputString, status, birthDate);

        // Falls der Benutzername existiert, bleibt der Benutzer auf der Ergänzungsseite
        if (usernameExists) {
            model.addAttribute("error", "Der Benutzername existiert bereits. Bitte wählen Sie einen anderen.");
            return "Profile Additions"; // Thymeleaf-Template für die Profilerweiterung
        }

        // Weiterleitung zum Profil bei erfolgreicher Aktualisierung
        return "redirect:/profile";
    }

    @GetMapping("/download-user-info")
    public void downloadUserInfo(HttpServletResponse response,
                                 @AuthenticationPrincipal OAuth2User user,
                                 OAuth2AuthenticationToken authentication
                                 ) {

        try {
            UserEntity benutzer = userService.getCurrentUser(user, authentication);

            // HTTP-Header setzen
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "attachment; filename=\"user_info.pdf\"");

            // PDF generieren
            userService.createPdf(response.getOutputStream(), benutzer);

        } catch (Exception e) {
            // Fehler-Handling
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/save-profile")
    public String saveProfile(
            @RequestParam("address") String address,
            @RequestParam("city") String city,
            @RequestParam("region") String region,
            @RequestParam("postalCode") String postalCode,
            @AuthenticationPrincipal OAuth2User user,
            OAuth2AuthenticationToken auth2AuthenticationToken

    ){
        UserEntity userEntity = userService.getCurrentUser(user, auth2AuthenticationToken);
        System.out.println(userEntity);

        PersonalInformation personalInformation = userEntity.getPersonalInformation();
        System.out.println(personalInformation);

        personalInformation.setAddress(address);
        personalInformation.setCity(city);
        personalInformation.setRegion(region);
        personalInformation.setPostalCode(postalCode);
        personalInformation.setUser(userEntity);
        userEntity.setPersonalInformation(personalInformation);

        userRepository.save(userEntity);

        return "redirect:/profile";
    }

//    @PostMapping("/send-2fa-code")
//    public String sendTwoFactorCode(
//            @AuthenticationPrincipal OAuth2User user,
//            OAuth2AuthenticationToken auth2AuthenticationToken
//    ) {
//        UserEntity userEntity = userService.getCurrentUser(user, auth2AuthenticationToken);
//
//        // Generiere einen 6-stelligen Code
//        String code = String.format("%06d", new Random().nextInt(999999));
//
//        // Speichere den Code und Ablaufzeit in der Datenbank
//        userEntity.setTwoFactorCode(code);
//        userEntity.setTwoFactorExpiry(LocalDateTime.now().plusMinutes(5)); // Ablaufzeit: 5 Minuten
//        userRepository.save(userEntity);
//
//        // Sende den Code per E-Mail (oder SMS, falls implementiert)
//        emailService.sendEmail(userEntity.getEmail(), "Your 2FA Code", "Your code is: " + code);
//
//        return "redirect:/verify-2fa";
//    }

}
