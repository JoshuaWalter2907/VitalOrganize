package com.springboot.vitalorganize.controller;

import com.springboot.vitalorganize.dto.ProfileAdditionData;
import com.springboot.vitalorganize.dto.ProfileRequest;
import com.springboot.vitalorganize.model.*;
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

@Controller
@AllArgsConstructor
public class ProfileController {

    private final UserService userService;
    private final ProfileService profileService;
    private final FriendRequestService friendRequestService;


    @GetMapping("/profile")
    public String profile(
            Model model,
            @AuthenticationPrincipal OAuth2User user,
            OAuth2AuthenticationToken authenticationToken,
            @RequestParam(value = "profileId", required = false) Long profileId
    ) {

        UserEntity currentUser = userService.getCurrentUser(user, authenticationToken);

        // Hole das Profil des Benutzers
        UserEntity profileUser = currentUser;
        if(profileId != null)
            profileUser= profileService.getProfileUser(user, authenticationToken, profileId);


        // Liste der Freunde des Benutzers
        List<UserEntity> friends = profileUser.getFriends();

        if (profileId == null) {
            List<UserEntity> blockedUsers = profileService.getBlockedUsers(currentUser);
            List<UserEntity> potentialFriends = profileService.getPotentialFriends(currentUser);
            List<FriendRequest> receivedRequests = profileService.getFriendRequests(currentUser);
            List<FriendRequest> sentRequests = profileService.getSentRequests(currentUser);

            model.addAttribute("blockedUsers", blockedUsers);
            model.addAttribute("potentialFriends", potentialFriends);
            model.addAttribute("friendRequests", receivedRequests);
            model.addAttribute("outgoingFriendRequests", sentRequests);
        }

        model.addAttribute("friends", friends);
        model.addAttribute("userEntity", profileUser);

        return "profile";
    }

    private Long getCurrentUserId(OAuth2User user, OAuth2AuthenticationToken authenticationToken) {
        return userService.getCurrentUser(user, authenticationToken).getId();
    }

    private String redirectToProfile() {
        return "redirect:/profile";
    }

    @PostMapping("/block/{id}")
    public String blockUser(@PathVariable Long id, @AuthenticationPrincipal OAuth2User user, OAuth2AuthenticationToken authenticationToken) {
        userService.blockUser(getCurrentUserId(user, authenticationToken), id);
        return redirectToProfile();
    }

    @PostMapping("/addFriend/{id}")
    public String addFriend(@PathVariable Long id, @AuthenticationPrincipal OAuth2User user, OAuth2AuthenticationToken authenticationToken) {
        userService.addFriend(getCurrentUserId(user, authenticationToken), id);
        return redirectToProfile();
    }

    @PostMapping("/unfriend/{id}")
    public String unfriendUser(@PathVariable Long id, @AuthenticationPrincipal OAuth2User user, OAuth2AuthenticationToken authenticationToken) {
        userService.unfriendUser(getCurrentUserId(user, authenticationToken), id);
        return redirectToProfile();
    }

    @PostMapping("/unblock/{id}")
    public String unblockUser(@PathVariable Long id, @AuthenticationPrincipal OAuth2User user, OAuth2AuthenticationToken authenticationToken) {
        userService.unblockUser(getCurrentUserId(user, authenticationToken), id);
        return redirectToProfile();
    }


    @PostMapping("/acceptRequest/{id}")
    public String acceptFriendRequest(@PathVariable Long id, @AuthenticationPrincipal OAuth2User user, OAuth2AuthenticationToken authenticationToken) {
        UserEntity userEntity = userService.getCurrentUser(user, authenticationToken);

        // Akzeptiere die Freundschaftsanfrage über den Service
        friendRequestService.acceptFriendRequest(id, userEntity);

        return "redirect:/profile";
    }

    @PostMapping("/rejectRequest/{id}")
    public String rejectFriendRequest(@PathVariable Long id, @AuthenticationPrincipal OAuth2User user, OAuth2AuthenticationToken authenticationToken) {
        // Aktuellen Benutzer abrufen
        UserEntity currentUser = userService.getCurrentUser(user, authenticationToken);

        // Freundschaftsanfrage ablehnen
        friendRequestService.rejectFriendRequest(id, currentUser);

        // Nach Ablehnung zurück zur Profil-Seite
        return "redirect:/profile";
    }


    @PostMapping("/cancelRequest/{id}")
    public String cancelFriendRequest(@PathVariable("id") Long requestId, @AuthenticationPrincipal OAuth2User user, OAuth2AuthenticationToken authenticationToken) {
        // Den aktuellen Benutzer abrufen
        UserEntity currentUser = userService.getCurrentUser(user, authenticationToken);

        // Freundschaftsanfrage abbrechen
        friendRequestService.cancelFriendRequest(requestId, currentUser);

        // Nach Abbrechen zurück zur Profil-Seite
        return "redirect:/profile";
    }






    @GetMapping("/profile-edit")
    public String profile(ProfileRequest profileRequest,
                          @RequestParam(value = "fa", required = false) boolean auth,
                          @AuthenticationPrincipal OAuth2User user,
                          OAuth2AuthenticationToken authentication, Model model, HttpServletRequest request,
                          HttpSession session) {

        String currentUrl = request.getRequestURI();
        session.setAttribute("uri", currentUrl);
        model.addAttribute("url", currentUrl);

        UserEntity profileData = profileService.getProfileData(profileRequest.getProfileId(), user, authentication);

        List<SubscriptionEntity> subscriptions = profileService.getSubscriptions(profileData);
        model.addAttribute("subscriptions", subscriptions);
        model.addAttribute("profile", profileData);
        model.addAttribute("isProfilePublic", profileData.isPublic());
        model.addAttribute("auth", auth);

        if ("premium".equals(profileRequest.getKind())) {
            List<TransactionSubscription> transactions = profileService.getTransactionHistory(profileData, profileRequest.getKind(),
                    profileRequest.getUsername(), profileRequest.getDatefrom(), profileRequest.getDateto(), profileRequest.getAmount());
            model.addAttribute("historysubscription", transactions);
        } else {
            List<Payment> payments = profileService.getFilteredPayments(profileData, profileRequest.getUsername(), profileRequest.getReason(),
                    profileRequest.getDatefrom(), profileRequest.getDateto(), profileRequest.getAmount());
            model.addAttribute("historysingle", payments);
        }

        String showSubscription = profileService.determineTab(profileRequest.getTab());
        model.addAttribute("showSubscription", showSubscription);

        return "private-profile";
    }


    @GetMapping("/profileaddition")
    public String profileAdditionGet(@RequestParam(name = "nousername", required = false) boolean nousername,
                                     @RequestParam(value = "fa", required = false) boolean auth,
                                     Model model,
                                     @AuthenticationPrincipal OAuth2User user,
                                     OAuth2AuthenticationToken authentication,
                                     HttpServletRequest request,
                                     HttpSession session
    ) {
        String email;
        String username;
        String birthdate;
        Boolean isPublic;
        if(auth){
            email = session.getAttribute("email").toString();
            username = session.getAttribute("inputString").toString();
            birthdate = session.getAttribute("birthDate").toString();
            isPublic= (Boolean) session.getAttribute("isPublic");
            model.addAttribute("email", email);
            model.addAttribute("username", username);
            model.addAttribute("birthday", birthdate);
            model.addAttribute("isPublic", isPublic);
        }

        String uri = request.getRequestURI();
        session.setAttribute("uri", uri);

        UserEntity userEntity = userService.getCurrentUser(user, authentication);

        if(userEntity.getProvider().equals("github")) {
            model.addAttribute("provider", userEntity.getProvider());
        }

        // Benutzerinformationen und bestehendes Profil abrufen
        ProfileAdditionData profileData = userService.getProfileAdditionData(user, authentication);

        if (profileData.isProfileComplete()) {
            return "redirect:/profile"; // Weiterleitung, wenn das Profil vollständig ist
        }


        // Daten für die View vorbereiten
        model.addAttribute("user", profileData.getUserEntity());
        model.addAttribute("birthDate", profileData.getBirthDate());
        model.addAttribute("auth", auth);


        return "Profile Additions"; // Thymeleaf-Template für die Profilerweiterung
    }

    @PostMapping("/profileaddition")
    public String profileAddition(@RequestParam("inputString") String inputString,
                                  @RequestParam("birthDate") String birthDate,
                                  @RequestParam(value = "email", required = false, defaultValue = "") String email,
                                  @AuthenticationPrincipal OAuth2User user,
                                  OAuth2AuthenticationToken authentication,
                                  Model model) {

        // Profilaktualisierung durchführen
        boolean usernameExists = profileService.updateUserProfile(user, authentication, inputString, birthDate, email);

        // Falls der Benutzername existiert, bleibt der Benutzer auf der Ergänzungsseite
        if (usernameExists) {
            model.addAttribute("error", "Der Benutzername existiert bereits. Bitte wählen Sie einen anderen.");
            return "Profile Additions"; // Thymeleaf-Template für die Profilerweiterung
        }

        // Weiterleitung zum Profil bei erfolgreicher Aktualisierung
        return "redirect:/profile";
    }

    @PostMapping("/save-profile")
    public String saveProfile(ProfileRequest profileRequest,
                              @AuthenticationPrincipal OAuth2User user,
                              OAuth2AuthenticationToken auth2AuthenticationToken) {

        // Aktuellen Benutzer holen
        UserEntity userEntity = userService.getCurrentUser(user, auth2AuthenticationToken);

        // Profilaktualisierung durch den UserService
        profileService.updateUserProfile(userEntity, profileRequest);

        return "redirect:/profile-edit"; // Nach erfolgreicher Speicherung zur Profilseite weiterleiten
    }

    @PostMapping("/profile-edit/delete")
    public String deleteProfile(
            @AuthenticationPrincipal OAuth2User user,
            OAuth2AuthenticationToken auth2AuthenticationToken
    ){

        UserEntity userEntity = userService.getCurrentUser(user, auth2AuthenticationToken);

        Boolean allDone = userService.deleteUser(userEntity);

        if(!allDone)
            return "redirect:/fund?delete=true";
        return "redirect:/logout";
    }
}
