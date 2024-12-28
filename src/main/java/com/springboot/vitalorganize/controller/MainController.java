package com.springboot.vitalorganize.controller;
import com.springboot.vitalorganize.dto.ProfileAdditionData;
import com.springboot.vitalorganize.model.*;
import com.springboot.vitalorganize.service.AuthenticationService;
import com.springboot.vitalorganize.service.PaypalService;
import com.springboot.vitalorganize.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;


@Controller
@AllArgsConstructor
public class MainController {

    private final UserService userService;
    private final AuthenticationService authenticationService;
    private final UserRepository userRepository;
    private final PaypalService paypalService;
    private final FriendRequestRepository friendRequestRepository;
    private final PaymentRepository paymentRepository;


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
    public String profile(
            Model model,
            @AuthenticationPrincipal OAuth2User user,
            OAuth2AuthenticationToken authenticationToken,
            @RequestParam(value = "profileId", required = false) Long profileId
    ) {

        UserEntity currentUser = userService.getCurrentUser(user, authenticationToken);

        if (profileId != null && profileId.equals(currentUser.getId())) {
            return "redirect:/profile"; // Leitet auf /profile ohne profileId-Parameter
        }

        UserEntity profileUser = (profileId == null) ? currentUser : userRepository.findById(profileId)
                .orElseThrow(() -> new RuntimeException("User with id " + profileId + " not found"));

        System.out.println(profileUser.getUsername());

        List<UserEntity> friends = profileUser.getFriends();

        System.out.println("Die profileId = " + profileId);

        if(profileId == null){
            System.out.println("Ich war hier");
            List<UserEntity> blockedUsers = currentUser.getBlockedUsers();
            List<UserEntity> allUsers = userRepository.findAll();

            List<FriendRequest> receivedRequests = currentUser.getReceivedFriendRequests();

            List<FriendRequest> sentRequests = currentUser.getSentFriendRequests().stream()
                    .filter(fr -> fr.getStatus() == FriendRequest.RequestStatus.PENDING)
                    .collect(Collectors.toList());

            List<UserEntity> potentialFriends = allUsers.stream()
                    .filter(u -> !u.equals(currentUser)) // Sich selbst ausschließen
                    .filter(u -> !friends.contains(u)) // Keine Freunde
                    .filter(u -> !blockedUsers.contains(u)) // Nicht blockiert von diesem Benutzer
                    .filter(u -> !u.getBlockedUsers().contains(currentUser)) // Nicht blockiert von der anderen Seite
                    .filter(u -> currentUser.getSentFriendRequests().stream()
                            .noneMatch(fr -> fr.getReceiver().equals(u) && fr.getStatus() == FriendRequest.RequestStatus.PENDING)) // Keine offene Anfrage
                    .toList();

            model.addAttribute("blockedUsers", blockedUsers);
            model.addAttribute("potentialFriends", potentialFriends);
            model.addAttribute("friendRequests", receivedRequests);
            model.addAttribute("outgoingFriendRequests", sentRequests);
        }

        model.addAttribute("friends", friends);

        model.addAttribute("userEntity", profileUser);

        return "profile";
    }

    @PostMapping("/block/{id}")
    public String blockUser(@PathVariable Long id, @AuthenticationPrincipal OAuth2User user, OAuth2AuthenticationToken authenticationToken) {
        Long currentUserId = userService.getCurrentUser(user, authenticationToken).getId();
        userService.blockUser(currentUserId, id);

        return "redirect:/profile";
    }

    @PostMapping("/addFriend/{id}")
    public String addFriend(@PathVariable Long id, @AuthenticationPrincipal OAuth2User user, OAuth2AuthenticationToken authenticationToken) {
        Long currentUserId = userService.getCurrentUser(user, authenticationToken).getId();
        userService.addFriend(currentUserId, id);
        return "redirect:/profile";
    }

    @PostMapping("/unfriend/{id}")
    public String unfriendUser(
            @PathVariable Long id,
            @AuthenticationPrincipal OAuth2User user,
            OAuth2AuthenticationToken authenticationToken) {
        Long currentUserId = userService.getCurrentUser(user, authenticationToken).getId();
        userService.unfriendUser(currentUserId, id);

        return "redirect:/profile";
    }

    @PostMapping("/unblock/{id}")
    public String unblockUser(
            @PathVariable Long id,
            @AuthenticationPrincipal OAuth2User user,
            OAuth2AuthenticationToken authenticationToken) {
        Long currentUserId = userService.getCurrentUser(user, authenticationToken).getId();
        userService.unblockUser(currentUserId, id);

        return "redirect:/profile";
    }

    @PostMapping("/acceptRequest/{id}")
    public String acceptFriendRequest(@PathVariable Long id, @AuthenticationPrincipal OAuth2User user, OAuth2AuthenticationToken authenticationToken) {
        UserEntity userEntity = userService.getCurrentUser(user, authenticationToken);
        FriendRequest friendRequest = friendRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Friend request not found"));

        // Freundschaftsanfrage akzeptieren
        friendRequest.setStatus(FriendRequest.RequestStatus.ACCEPTED);
        userEntity.getFriends().add(friendRequest.getSender());
        friendRequest.getSender().getFriends().add(userEntity);
        friendRequestRepository.save(friendRequest);
        userRepository.save(userEntity);

        friendRequestRepository.delete(friendRequest);

        return "redirect:/profile";
    }

    @PostMapping("/rejectRequest/{id}")
    public String rejectFriendRequest(@PathVariable Long id, @AuthenticationPrincipal OAuth2User user, OAuth2AuthenticationToken authenticationToken) {
        UserEntity userEntity = userService.getCurrentUser(user, authenticationToken);
        FriendRequest friendRequest = friendRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Friend request not found"));

        // Freundschaftsanfrage ablehnen
        friendRequest.setStatus(FriendRequest.RequestStatus.REJECTED);
        friendRequestRepository.save(friendRequest);

        friendRequestRepository.delete(friendRequest);

        return "redirect:/profile";
    }

    @PostMapping("/cancelRequest/{id}")
    public String cancelFriendRequest(
            @PathVariable("id") Long requestId,
            @AuthenticationPrincipal OAuth2User user,
            OAuth2AuthenticationToken authenticationToken) {

        // Den aktuellen Benutzer ermitteln
        UserEntity currentUser = userService.getCurrentUser(user, authenticationToken);

        // Die Freundschaftsanfrage suchen
        FriendRequest request = friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Friend request not found"));

        // Überprüfen, ob der aktuelle Benutzer der Absender der Anfrage ist
        if (!request.getSender().getId().equals(currentUser.getId())) {
            throw new RuntimeException("You are not authorized to cancel this request");
        }

        // Anfrage löschen
        friendRequestRepository.delete(request);

        return "redirect:/profile"; // Oder zu einer anderen Seite weiterleiten
    }






    @GetMapping("/profile-edit")
    public String profile(@RequestParam(value = "theme", defaultValue = "light") String theme,
                          @RequestParam(value = "lang", defaultValue = "en") String lang,
                          @RequestParam(value ="profileId", required = false) Long profileId,
                          @RequestParam(value = "fa", required = false) boolean auth,
                          @RequestParam(value = "tab", defaultValue = "general") String tab,
                          @RequestParam(value = "kind", defaultValue = "premium")String kind,
                          @AuthenticationPrincipal OAuth2User user,
                          OAuth2AuthenticationToken authentication,
                          Model model,
                          HttpServletRequest request,
                          HttpSession session,
                          ModelMap modelMap) {

        String currentUrl = request.getRequestURI();
        session.setAttribute("uri", currentUrl);
        model.addAttribute("url", currentUrl);

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

        List<SubscriptionEntity> subscriptions = profileData.getSubscriptions();
        Collections.reverse(subscriptions);

        boolean isProfilePublic = profileData.isPublic();
        System.out.println("Ist das Profil public: " + isProfilePublic);

        model.addAttribute("isProfilePublic", isProfilePublic);
        model.addAttribute("profile", profileData);
        System.out.println(auth);
        model.addAttribute("auth", auth);
        model.addAttribute("subscriptions", subscriptions);

        if ("subscription".equals(tab)) {
            // Abonnement-Informationen anzeigen
            model.addAttribute("showSubscription", "subscription");
        } else if("paymenthistory".equals(tab)) {
            // Allgemeine Informationen anzeigen
            model.addAttribute("showSubscription", "paymenthistory");
        }else {
            model.addAttribute("showSubscription", "general");
        }

        if("premium".equals(kind) && profileData.getLatestSubscription() != null) {
            System.out.println("Ich war hier");
            model.addAttribute("historysubscription", paypalService.getTransactionsForSubscription(profileData.getLatestSubscription().getSubscriptionId()));
        }else {
            model.addAttribute("historysingle", paymentRepository.findAllByUser(profileData));
        }


        return "private-profile"; // Gibt die View "private-profile.html" zurück
    }

    @GetMapping("/profileaddition")
    public String profileAdditionGet(@RequestParam(value = "theme", defaultValue = "light") String theme,
                                     @RequestParam(name = "nousername", required = false) boolean nousername,
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
        model.addAttribute("themeCss", userService.getThemeCss(theme));
        model.addAttribute("auth", auth);


        return "Profile Additions"; // Thymeleaf-Template für die Profilerweiterung
    }

    @PostMapping("/profileaddition")
    public String profileAddition(@RequestParam("inputString") String inputString,
                                  @RequestParam("birthDate") String birthDate,
                                  @RequestParam(value = "email", required = false) String email,
                                  @AuthenticationPrincipal OAuth2User user,
                                  OAuth2AuthenticationToken authentication,
                                  Model model) {

        // Profilaktualisierung durchführen
        boolean usernameExists = userService.updateUserProfile(user, authentication, inputString, true, birthDate, email);

        // Falls der Benutzername existiert, bleibt der Benutzer auf der Ergänzungsseite
        if (usernameExists) {
            model.addAttribute("error", "Der Benutzername existiert bereits. Bitte wählen Sie einen anderen.");
            return "Profile Additions"; // Thymeleaf-Template für die Profilerweiterung
        }

        // Weiterleitung zum Profil bei erfolgreicher Aktualisierung
        return "redirect:/profile";
    }

    @PostMapping("/save-profile")
    public String saveProfile(
            @RequestParam("address") String address,
            @RequestParam("city") String city,
            @RequestParam("region") String region,
            @RequestParam("postalCode") String postalCode,
            @RequestParam("surname") String surname,
            @RequestParam("name") String name,
            @RequestParam(value = "public-private-toggle", defaultValue = "off") String publicPrivateToggle,
            @AuthenticationPrincipal OAuth2User user,
            OAuth2AuthenticationToken auth2AuthenticationToken

    ){
        UserEntity userEntity = userService.getCurrentUser(user, auth2AuthenticationToken);
        System.out.println(userEntity);

        PersonalInformation personalInformation = userEntity.getPersonalInformation();
        System.out.println(personalInformation);

        if("on".equals(publicPrivateToggle)) {
            userEntity.setPublic(true);
        }else {
            userEntity.setPublic(false);
        }
        personalInformation.setAddress(address);
        personalInformation.setCity(city);
        personalInformation.setRegion(region);
        personalInformation.setPostalCode(postalCode);
        personalInformation.setFirstName(surname);
        personalInformation.setLastName(name);
        personalInformation.setUser(userEntity);
        userEntity.setPersonalInformation(personalInformation);

        userRepository.save(userEntity);

        return "redirect:/profile-edit";
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
        System.out.println(uri);
        if("/profileaddition".equals(uri)) {
            session.setAttribute("email", email);
            session.setAttribute("inputString", inputString);
            session.setAttribute("birthDate", birthDate);
            session.setAttribute("isPublic", isPublic);
        }

        UserEntity userEntity = userService.getCurrentUser(user, auth2AuthenticationToken);

        // Generiere einen 6-stelligen Code
        String code = String.format("%06d", new Random().nextInt(999999));

        // Speichere den Code und Ablaufzeit in der Datenbank
        userEntity.setTwoFactorCode(code);
        userEntity.setTwoFactorExpiry(LocalDateTime.now().plusMinutes(5)); // Ablaufzeit: 5 Minuten
        userRepository.save(userEntity);

        if(userEntity.getProvider().equals("github") && userEntity.getSendtoEmail() != null)
            email = userEntity.getSendtoEmail();

        System.out.println(email);
        // Sende den Code per E-Mail (oder SMS, falls implementiert)
        paypalService.sendEmail(email, "Your 2FA Code", "Your code is: " + code);

        return "redirect:" + uri + "?fa=true";

    }

    @PostMapping("/verify-2fa")
    public String verifyTwoFactorCode(
            @AuthenticationPrincipal OAuth2User user,
            OAuth2AuthenticationToken auth2AuthenticationToken,
            @RequestParam(value = "digit1", required = false) String digit1,
            @RequestParam(value = "digit2", required = false) String digit2,
            @RequestParam(value = "digit3", required = false) String digit3,
            @RequestParam(value = "digit4", required = false) String digit4,
            @RequestParam(value = "digit5", required = false) String digit5,
            @RequestParam(value = "digit6", required = false) String digit6,
            HttpSession session,
            HttpServletResponse response
    ) throws IOException {

        String email = "";
        String username = "";
        String birthdate = "";
        Boolean isPublic = false;

        String uri = (String) session.getAttribute("uri");
        if(uri.equals("/profileaddition")){
            email = session.getAttribute("email").toString();
            username = session.getAttribute("inputString").toString();
            birthdate = session.getAttribute("birthDate").toString();
            isPublic = (Boolean) session.getAttribute("isPublic");
        }

        String code = digit1 + digit2 + digit3 + digit4 + digit5 + digit6;
        System.out.println("der code: " + code);

        UserEntity userEntity = userService.getCurrentUser(user, auth2AuthenticationToken);

        // Überprüfe den Code und die Ablaufzeit
        if (userEntity.getTwoFactorCode().equals(code) &&
                userEntity.getTwoFactorExpiry().isAfter(LocalDateTime.now())) {

            // Authentifizierung erfolgreich, setze den Code zurück
            userEntity.setTwoFactorCode(null);
            userEntity.setTwoFactorExpiry(null);
            userRepository.save(userEntity);

            session.setAttribute("2fa_verified", true);

            if(uri.equals("/profile-edit")) {
                paypalService.createPdf(userEntity);
                return "redirect:/profile-edit";
            }else if("/profileaddition".equals(uri)) {
                return "forward:/profileaddition?inputString=" + username + "&isPublic=" + isPublic + "&birthDate=" + birthdate + "&email=" + email;
            }
        }

        return "redirect:/profile-edit";

    }

    @PostMapping("/profile-edit/delete")
    public String deleteProfile(
            @AuthenticationPrincipal OAuth2User user,
            OAuth2AuthenticationToken auth2AuthenticationToken,
            HttpServletRequest request
    ){

        UserEntity userEntity = userService.getCurrentUser(user, auth2AuthenticationToken);

        userService.deleteUser(userEntity);

        return "redirect:/logout";
    }



}
