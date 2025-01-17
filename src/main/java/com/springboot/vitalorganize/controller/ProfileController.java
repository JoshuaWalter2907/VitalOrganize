package com.springboot.vitalorganize.controller;

import com.springboot.vitalorganize.model.Profile.*;
import com.springboot.vitalorganize.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

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


    /**
     * Endpoint der für Änderungen im Profil des Nutzers zuständig ist
     * @param profileEditRequestDTO Informationen, die benötigt werden
     * @param model das Model für die View
     * @param request für die Rücksprungadresse
     * @param session für gespeicherte Informationen in der Session
     * @return profileSeite
     */
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


    /**
     * Seite für zusätzliche Informationen, die zu registrierung eines Benuters benötigt werden
     * @param registrationAdditionRequestDTO Informationen dazu
     * @param model das Model für die View
     * @param request für die Rücksprungadresse
     * @param session für gespeicherte Informationen in der Session
     * @return Registration Additions
     */
    @GetMapping("/additional-registration")
    public String profileAdditionGet(RegistrationAdditionRequestDTO registrationAdditionRequestDTO,
                                     Model model,
                                     HttpServletRequest request,
                                     HttpSession session) {
        RegistrationAdditionResponseDTO registrationAdditionResponseDTO = profileService.prepareRegistrationAdditionPage(registrationAdditionRequestDTO, request, session);
        if(registrationAdditionResponseDTO.isProfileComplete())
            return "redirect:/profile";

        model.addAttribute("RegistrationAdditionData", registrationAdditionResponseDTO);
        return "profile/Registration Additions";
    }


    /**
     * Endpoint um zusätzliche Informationen während des Registriervorgangs zu speichern
     * @param registrationAdditionResponseDTO Informationen
     * @return Weiterleitung zum Profil
     */
    @PostMapping("/additional-registration")
    public String profileAddition(
            RegistrationAdditionResponseDTO registrationAdditionResponseDTO
    ) {
        profileService.updateUserProfile(registrationAdditionResponseDTO);

        return "redirect:/profile";
    }

    /**
     * Speichert Änderungen am Profil des Benutzers.
     *
     * @param profileEditRequestDTO die aktualisierten Profildaten
     * @return eine Weiterleitung zur Profilbearbeitungsseite
     */
    @PostMapping("/save-profile")
    public String saveProfile(ProfileEditRequestDTO profileEditRequestDTO) {

        profileService.updateUserProfile(profileEditRequestDTO);

        return "redirect:/profile-edit";
    }


    /**
     * Löscht das Profil des Benutzers.
     * @return eine Weiterleitung basierend auf dem Löschstatus
     */
    @PostMapping("/profile-edit/delete")
    public String deleteProfile() {

        Boolean allDone = userService.deleteUser();

        if (!allDone) {
            return "redirect:/fund?delete=true";
        }
        return "redirect:/logout";
    }

    /**
     * Hilfsmethode für Freundschaftsanfragen
     * @return Profilepage
     */
    private String redirectToProfile() {


        return "redirect:/profile";
    }

}
