package com.springboot.vitalorganize.controller;

import com.springboot.vitalorganize.model.ProfileAdditionData;
import com.springboot.vitalorganize.model.ProfileRequest;
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
     *
     * @param model                das Model für die View
     * @param user                 der aktuell authentifizierte Benutzer
     * @param authenticationToken  das OAuth2-Authentifizierungstoken
     * @param profileId            die ID des zu betrachtenden Profils (optional)
     * @return die Profil-View
     */
    @GetMapping("/profile")
    public String profile(Model model, @AuthenticationPrincipal OAuth2User user,
                          OAuth2AuthenticationToken authenticationToken,
                          @RequestParam(value = "profileId", required = false) Long profileId) {

        // Hole den aktuell authentifizierten Benutzer
        UserEntity currentUser = userService.getCurrentUser(user, authenticationToken);

        // Bestimme, ob das Profil des aktuellen Benutzers oder eines anderen angezeigt werden soll
        UserEntity profileUser = profileId != null ?
                profileService.getProfileUser(user, authenticationToken, profileId) : currentUser;

        // Füge Freunde und zusätzliche Informationen zur Model hinzu, falls kein Fremdprofil betrachtet wird
        if (profileId == null) {
            model.addAttribute("blockedUsers", profileService.getBlockedUsers(currentUser));
            model.addAttribute("potentialFriends", profileService.getPotentialFriends(currentUser));
            model.addAttribute("friendRequests", profileService.getFriendRequests(currentUser));
            model.addAttribute("outgoingFriendRequests", profileService.getSentRequests(currentUser));
        }

        // Füge Freunde und das Benutzerprofil zur Model hinzu
        model.addAttribute("friends", profileUser.getFriends());
        model.addAttribute("userEntity", profileUser);

        return "profile/profile";
    }

    /**
     * Blockiert einen Benutzer.
     *
     * @param id                  die ID des zu blockierenden Benutzers
     * @param user                der aktuell authentifizierte Benutzer
     * @param authenticationToken das OAuth2-Authentifizierungstoken
     * @return Weiterleitung zur Profilseite
     */
    @PostMapping("/block/{id}")
    public String blockUser(@PathVariable Long id, @AuthenticationPrincipal OAuth2User user,
                            OAuth2AuthenticationToken authenticationToken) {
        userService.blockUser(getCurrentUserId(user, authenticationToken), id);
        return redirectToProfile();
    }

    /**
     * Sendet eine Freundschaftsanfrage.
     */
    @PostMapping("/addFriend/{id}")
    public String addFriend(@PathVariable Long id, @AuthenticationPrincipal OAuth2User user,
                            OAuth2AuthenticationToken authenticationToken) {
        userService.addFriend(getCurrentUserId(user, authenticationToken), id);
        return redirectToProfile();
    }

    /**
     * Entfernt einen Benutzer aus der Freundesliste.
     */
    @PostMapping("/unfriend/{id}")
    public String unfriendUser(@PathVariable Long id, @AuthenticationPrincipal OAuth2User user,
                               OAuth2AuthenticationToken authenticationToken) {
        userService.unfriendUser(getCurrentUserId(user, authenticationToken), id);
        return redirectToProfile();
    }

    /**
     * Entblockiert einen Benutzer.
     */
    @PostMapping("/unblock/{id}")
    public String unblockUser(@PathVariable Long id, @AuthenticationPrincipal OAuth2User user,
                              OAuth2AuthenticationToken authenticationToken) {
        userService.unblockUser(getCurrentUserId(user, authenticationToken), id);
        return redirectToProfile();
    }


    /**
     * Akzeptiert eine eingehende Freundschaftsanfrage.
     */
    @PostMapping("/acceptRequest/{id}")
    public String acceptFriendRequest(@PathVariable Long id, @AuthenticationPrincipal OAuth2User user,
                                      OAuth2AuthenticationToken authenticationToken) {
        UserEntity userEntity = userService.getCurrentUser(user, authenticationToken);
        friendRequestService.acceptFriendRequest(id, userEntity);
        return "redirect:/profile";
    }

    /**
     * Lehnt eine eingehende Freundschaftsanfrage ab.
     */
    @PostMapping("/rejectRequest/{id}")
    public String rejectFriendRequest(@PathVariable Long id, @AuthenticationPrincipal OAuth2User user,
                                      OAuth2AuthenticationToken authenticationToken) {
        UserEntity currentUser = userService.getCurrentUser(user, authenticationToken);
        friendRequestService.rejectFriendRequest(id, currentUser);
        return "redirect:/profile";
    }


    /**
     * Bricht eine ausgehende Freundschaftsanfrage ab.
     */
    @PostMapping("/cancelRequest/{id}")
    public String cancelFriendRequest(@PathVariable("id") Long requestId, @AuthenticationPrincipal OAuth2User user,
                                      OAuth2AuthenticationToken authenticationToken) {
        UserEntity currentUser = userService.getCurrentUser(user, authenticationToken);
        friendRequestService.cancelFriendRequest(requestId, currentUser);
        return "redirect:/profile";
    }


    /**
     * Zeigt die Profilbearbeitungsseite an und lädt die erforderlichen Profildaten.
     *
     * @param profileRequest die Anfragedaten für das Profil
     * @param auth gibt an, ob eine Zwei-Faktor-Authentifizierung verwendet wird (optional)
     * @param user der authentifizierte OAuth2-Benutzer (optional)
     * @param authentication das Authentifizierungs-Token für den OAuth2-Benutzer
     * @param model das UI-Modell, in das die Profildaten eingefügt werden
     * @param request die aktuelle HTTP-Anfrage
     * @param session die aktuelle HTTP-Session
     * @return der Name des Thymeleaf-Templates für die Profilbearbeitungsseite
     */
    @GetMapping("/profile-edit")
    public String profile(ProfileRequest profileRequest,
                          @RequestParam(value = "fa", required = false) boolean auth,
                          @AuthenticationPrincipal OAuth2User user,
                          OAuth2AuthenticationToken authentication,
                          Model model,
                          HttpServletRequest request,
                          HttpSession session) {

        // Aktuelle URL in der Sitzung speichern und für die View bereitstellen
        String currentUrl = request.getRequestURI();
        session.setAttribute("uri", currentUrl);
        model.addAttribute("url", currentUrl);

        // Profildaten und Abonnements für den Benutzer abrufen
        UserEntity profileData = profileService.getProfileData(profileRequest.getProfileId(), user, authentication);
        List<SubscriptionEntity> subscriptions = profileService.getSubscriptions(profileData);

        // Profildaten und Abonnements in das Modell einfügen
        model.addAttribute("subscriptions", subscriptions);
        model.addAttribute("profile", profileData);
        model.addAttribute("isProfilePublic", profileData.isPublic());
        model.addAttribute("auth", auth);
        model.addAttribute("kind", profileRequest.getKind());

        // Transaktions- oder Zahlungshistorie basierend auf dem Profiltyp abrufen
        if ("premium".equals(profileRequest.getKind())) {
            List<TransactionSubscription> transactions = profileService.getTransactionHistory(
                    profileData,
                    profileRequest.getKind(),
                    profileRequest.getUsername(),
                    profileRequest.getDatefrom(),
                    profileRequest.getDateto(),
                    profileRequest.getAmount());
            model.addAttribute("historysubscription", transactions);
        } else {
            List<Payment> payments = profileService.getFilteredPayments(
                    profileData,
                    profileRequest.getUsername(),
                    profileRequest.getReason(),
                    profileRequest.getDatefrom(),
                    profileRequest.getDateto(),
                    profileRequest.getAmount());
            model.addAttribute("historysingle", payments);
        }

        // Sichtbaren Tab bestimmen und zum Modell hinzufügen
        String showSubscription = profileService.determineTab(profileRequest.getTab());
        model.addAttribute("showSubscription", showSubscription);

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
    public String saveProfile(ProfileRequest profileRequest,
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
