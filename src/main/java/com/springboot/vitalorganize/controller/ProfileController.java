package com.springboot.vitalorganize.controller;

import com.springboot.vitalorganize.model.*;
import com.springboot.vitalorganize.entity.*;
import com.springboot.vitalorganize.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller zur Verwaltung von Benutzerprofilen und verwandten Aktionen wie Freundschaftsanfragen,
 * Profilbearbeitung und Blockierungsfunktionen.
 */
@Controller
@AllArgsConstructor
public class ProfileController {

    private final UserService userService;
    private final ProfileService profileService;
    private final FriendRequestService friendRequestService;


    /**
     * Zeigt die Profilseite eines Benutzers an.
     * @param profileRequestDTO DTO für alle Informationen, die benötigt werden um das Profile anzuzeigen
     * @param model das Model für die View
     * @return die Profil-View
     */
    @GetMapping("/profile")
    public String profile(
            ProfileRequestDTO profileRequestDTO,
            Model model
    ) {
        ProfileResponseDTO profileResponseDTO = profileService.prepareProfilePage(profileRequestDTO);
        model.addAttribute("profileData", profileResponseDTO);
        return "profile/profile";
    }

    /**
     * Blockiert einen Benutzer
     * @param friendRequestDTO Enthält die Informationen des zu blockierenden Users
     * @return Profilpage
     */
    @PostMapping("/block/{id}")
    public String blockUser(
            FriendStatusRequestDTO friendRequestDTO
    ) {
        userService.blockUser(friendRequestDTO);
        return redirectToProfile();
    }

    /**
     * Befreundet einen Benutzer
     * @param friendStatusRequestDTO Enthält die Informationen des zu befreundenden Users
     * @return Profilpage
     */
    @PostMapping("/addFriend/{id}")
    public String addFriend(FriendStatusRequestDTO friendStatusRequestDTO) {
        userService.addFriend(friendStatusRequestDTO);
        return redirectToProfile();
    }

    /**
     * Entfreundet einen Benutzer
     * @param friendStatusRequestDTO Enthält die Informationen des zu entfreundung Users
     * @return Profilpage
     */
    @PostMapping("/unfriend/{id}")
    public String unfriendUser(FriendStatusRequestDTO friendStatusRequestDTO) {
        userService.unfriendUser(friendStatusRequestDTO);
        return redirectToProfile();
    }

    /**
     * Blockiert einen Nutzer
     * @param friendStatusRequestDTO Enthält die Informationen um einen Nutzer zu blockiern
     * @return ProfilePage
     */
    @PostMapping("/unblock/{id}")
    public String unblockUser(FriendStatusRequestDTO friendStatusRequestDTO) {
        userService.unblockUser(friendStatusRequestDTO);
        return redirectToProfile();
    }


    /**
     * Akzeptiert die Freundschaftsanfrage einen Nutzer
     * @param friendStatusRequestDTO Enthält die Informationen um eine Anfrage zu akzeptieren
     * @return ProfilePage
     */
    @PostMapping("/acceptRequest/{id}")
    public String acceptFriendRequest(FriendStatusRequestDTO friendStatusRequestDTO) {
        friendRequestService.acceptFriendRequest(friendStatusRequestDTO);
        return "redirect:/profile";
    }

    /**
     * Lehnt die Freundschaftsanfrage einen Nutzers ab
     * @param friendStatusRequestDTO Enthält die Informationen um eine Anfrage abzulehnen
     * @return ProfilePage
     */
    @PostMapping("/rejectRequest/{id}")
    public String rejectFriendRequest(FriendStatusRequestDTO friendStatusRequestDTO) {
        friendRequestService.rejectFriendRequest(friendStatusRequestDTO);
        return "redirect:/profile";
    }


    /**
     * Löscht eine ausgehende Freundschaftsanfrage
     * @param friendStatusRequestDTO Informationen die zur Löschung benötigt werden
     * @return Profilepage
     */
    @PostMapping("/cancelRequest/{id}")
    public String cancelFriendRequest(FriendStatusRequestDTO friendStatusRequestDTO) {
        friendRequestService.cancelFriendRequest(friendStatusRequestDTO);
        return "redirect:/profile";
    }



    @GetMapping("/profile-edit")
    public String profile(ProfileEditRequestDTO profileEditRequestDTO,
                          Model model,
                          HttpServletRequest request,
                          HttpSession session
    ) {
        ProfileEditResponseDTO profileEditResponseDTO = profileService.prepareProfileEditPage(profileEditRequestDTO, request, session);
        model.addAttribute("ProfileEditData", profileEditResponseDTO);

        return "profile/private-profile";
    }


    @GetMapping("/profileaddition")
    public String profileAdditionGet(@RequestParam(name = "nousername", required = false) boolean nousername,
                                     @RequestParam(value = "fa", required = false) boolean auth,
                                     Model model,
                                     @AuthenticationPrincipal OAuth2User user,
                                     OAuth2AuthenticationToken authentication,
                                     HttpServletRequest request,
                                     HttpSession session) {

        // Authentifizierungsdetails aus der Sitzung abrufen und für die View bereitstellen
        if (auth) {
            String email = session.getAttribute("email").toString();
            String username = session.getAttribute("inputString").toString();
            String birthdate = session.getAttribute("birthDate").toString();
            Boolean isPublic = (Boolean) session.getAttribute("isPublic");
            model.addAttribute("email", email);
            model.addAttribute("username", username);
            model.addAttribute("birthday", birthdate);
            model.addAttribute("isPublic", isPublic);
        }

        // Aktuelle URL speichern und Profildaten abrufen
        String uri = request.getRequestURI();
        session.setAttribute("uri", uri);
        UserEntity userEntity = userService.getCurrentUser(user, authentication);

        // Anbieterinformationen und Profilerweiterungsdaten zur View hinzufügen
        if (userEntity.getProvider().equals("github")) {
            model.addAttribute("provider", userEntity.getProvider());
        }
        ProfileAdditionData profileData = userService.getProfileAdditionData(user, authentication);

        // Weiterleitung, wenn das Profil vollständig ist
        if (profileData.isProfileComplete()) {
            return "redirect:/profile";
        }

        // Profildaten zur View hinzufügen
        model.addAttribute("user", profileData.getUserEntity());
        model.addAttribute("birthDate", profileData.getBirthDate());
        model.addAttribute("auth", auth);

        return "profile/Profile Additions";
    }


    /**
     * Verarbeitet das Absenden der Profilerweiterung.
     *
     * @param inputString der eingegebene Benutzername
     * @param birthDate das eingegebene Geburtsdatum
     * @param email die eingegebene E-Mail-Adresse (optional)
     * @param user der authentifizierte OAuth2-Benutzer
     * @param authentication das Authentifizierungs-Token für den OAuth2-Benutzer
     * @param model das UI-Modell, um Fehlermeldungen an die View zu übergeben
     * @return der Name des Templates oder eine Weiterleitung
     */
    @PostMapping("/profileaddition")
    public String profileAddition(@RequestParam(value = "inputString") String inputString,
                                  @RequestParam(value = "birthDate") String birthDate,
                                  @RequestParam(value = "email", required = false, defaultValue = "") String email,
                                  @AuthenticationPrincipal OAuth2User user,
                                  OAuth2AuthenticationToken authentication,
                                  Model model) {

        // Profil aktualisieren und Benutzername überprüfen
        boolean usernameExists = profileService.updateUserProfile(user, authentication, inputString, birthDate, email);

        // Fehlerbehandlung bei vorhandenem Benutzernamen
        if (usernameExists) {
            model.addAttribute("error", "Der Benutzername existiert bereits. Bitte wählen Sie einen anderen.");
            return "Profile Additions";
        }

        // Weiterleitung zum Profil bei erfolgreicher Aktualisierung
        return "redirect:/profile";
    }

    /**
     * Speichert Änderungen am Profil des Benutzers.
     *
     * @param profileRequest die aktualisierten Profildaten
     * @param user der authentifizierte OAuth2-Benutzer
     * @param auth2AuthenticationToken das Authentifizierungs-Token für den OAuth2-Benutzer
     * @return eine Weiterleitung zur Profilbearbeitungsseite
     */
    @PostMapping("/save-profile")
    public String saveProfile(ProfileEditRequestDTO profileRequest,
                              @AuthenticationPrincipal OAuth2User user,
                              OAuth2AuthenticationToken auth2AuthenticationToken) {

        // Benutzer-Entity abrufen und Profil aktualisieren
        UserEntity userEntity = userService.getCurrentUser(user, auth2AuthenticationToken);
        profileService.updateUserProfile(userEntity, profileRequest);

        return "redirect:/profile-edit";
    }


    /**
     * Löscht das Profil des Benutzers.
     *
     * @param user der authentifizierte OAuth2-Benutzer
     * @param auth2AuthenticationToken das Authentifizierungs-Token für den OAuth2-Benutzer
     * @return eine Weiterleitung basierend auf dem Löschstatus
     */
    @PostMapping("/profile-edit/delete")
    public String deleteProfile(
            @AuthenticationPrincipal OAuth2User user,
            OAuth2AuthenticationToken auth2AuthenticationToken) {

        // Benutzer-Entity abrufen und Profil löschen
        UserEntity userEntity = userService.getCurrentUser(user, auth2AuthenticationToken);
        Boolean allDone = userService.deleteUser(userEntity);

        // Weiterleitung abhängig vom Löschstatus
        if (!allDone) {
            return "redirect:/fund?delete=true";
        }
        return "redirect:/logout";
    }

    private Long getCurrentUserId(OAuth2User user, OAuth2AuthenticationToken authenticationToken) {
        return userService.getCurrentUser(user, authenticationToken).getId();
    }

    private String redirectToProfile() {
        return "redirect:/profile";
    }

}
