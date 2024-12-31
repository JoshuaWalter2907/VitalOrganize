package com.springboot.vitalorganize.service;

import com.springboot.vitalorganize.dto.ProfileRequest;
import com.springboot.vitalorganize.model.*;
import com.springboot.vitalorganize.repository.UserRepository;
import com.springboot.vitalorganize.service.repositoryhelper.PaymentRepositoryService;
import com.springboot.vitalorganize.service.repositoryhelper.UserRepositoryService;
import lombok.AllArgsConstructor;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ProfileService {

    private final UserRepositoryService userRepositoryService;
    private final UserService userService;
    private final PaypalService paypalService;
    private final PaymentRepositoryService paymentRepositoryService;

    public UserEntity getProfileUser(OAuth2User user, OAuth2AuthenticationToken authenticationToken, Long profileId) {
        UserEntity currentUser = userService.getCurrentUser(user, authenticationToken);

        // Überprüfe, ob das Profil des aktuellen Benutzers angezeigt wird
        if (profileId != null && profileId.equals(currentUser.getId())) {
            return currentUser; // Wenn das Profil des aktuellen Benutzers aufgerufen wird, gibt es das Profil zurück
        }

        // Ansonsten den Benutzer mit der übergebenen Profil-ID suchen
        return userRepositoryService.findUserById(profileId);
    }

    public List<UserEntity> getPotentialFriends(UserEntity currentUser) {
        List<UserEntity> blockedUsers = currentUser.getBlockedUsers();
        List<UserEntity> allUsers = userRepositoryService.findUsersByIds(List.of()); // Hol alle Benutzer aus dem RepositoryService

        return allUsers.stream()
                .filter(u -> !u.equals(currentUser)) // Sich selbst ausschließen
                .filter(u -> !currentUser.getFriends().contains(u)) // Keine Freunde
                .filter(u -> !blockedUsers.contains(u)) // Nicht blockiert von diesem Benutzer
                .filter(u -> !u.getBlockedUsers().contains(currentUser)) // Nicht blockiert von der anderen Seite
                .filter(u -> currentUser.getSentFriendRequests().stream()
                        .noneMatch(fr -> fr.getReceiver().equals(u) && fr.getStatus() == FriendRequest.RequestStatus.PENDING)) // Keine offene Anfrage
                .collect(Collectors.toList());
    }

    public List<FriendRequest> getFriendRequests(UserEntity currentUser) {
        return currentUser.getReceivedFriendRequests();
    }

    public List<FriendRequest> getSentRequests(UserEntity currentUser) {
        return currentUser.getSentFriendRequests().stream()
                .filter(fr -> fr.getStatus() == FriendRequest.RequestStatus.PENDING)
                .collect(Collectors.toList());
    }

    public List<UserEntity> getBlockedUsers(UserEntity currentUser) {
        return currentUser.getBlockedUsers();
    }

    public UserEntity getProfileData(Long profileId, OAuth2User user, OAuth2AuthenticationToken authentication) {
        UserEntity profileData;
        if (profileId == null) {
            profileData = userService.getProfileData(user, authentication);
        } else {
            profileData = userService.getUserById(profileId);
        }

        return profileData;
    }

    public List<SubscriptionEntity> getSubscriptions(UserEntity profileData) {
        List<SubscriptionEntity> subscriptions = profileData.getSubscriptions();
        Collections.reverse(subscriptions);
        return subscriptions;
    }

    public List<TransactionSubscription> getTransactionHistory(UserEntity profileData, String kind, String username, LocalDate datefrom, LocalDate dateto, Long amount) {
        if ("premium".equals(kind) && profileData.getLatestSubscription() != null) {
            List<TransactionSubscription> transactions = paypalService.getTransactionsForSubscription(
                    profileData.getLatestSubscription().getSubscriptionId()
            );
            return paypalService.filterTransactions(transactions, username, datefrom, dateto, amount);
        }
        return null;  // Alternativ: kann auch eine Liste von Zahlungen zurückgeben
    }

    public List<Zahlung> getFilteredPayments(UserEntity profileData, String username, String reason, LocalDate datefrom, LocalDate dateto, Long amount) {
        List<Zahlung> payments = paymentRepositoryService.findPaymentsByUser(profileData);
        return paypalService.filterPayments(payments, username, reason, datefrom, dateto, amount);
    }

    public String determineTab(String tab) {
        // Logik zur Bestimmung des aktiven Tabs
        if ("subscription".equals(tab)) {
            return "subscription";
        } else if ("paymenthistory".equals(tab)) {
            return "paymenthistory";
        }
        return "general"; // Standardwert
    }

    public boolean updateUserProfile(OAuth2User user, OAuth2AuthenticationToken authentication, String username, String birthdate, String email) {
        // Benutzerdaten abrufen
        UserEntity currentUser = userService.getCurrentUser(user,authentication);

        // Überprüfen, ob der Benutzername bereits existiert
        if (userRepositoryService.findAllUsers().stream()
                .anyMatch(u -> u.getUsername() != null && u.getUsername().equals(username))) {
            return true;  // Benutzername existiert bereits
        }

        // Profil aktualisieren
        currentUser.setUsername(username);
        currentUser.setBirthday(LocalDate.parse(birthdate));  // Beispiel: Geburtsdatum setzen
        if(!email.isBlank())
            currentUser.setEmail(email);
        userRepositoryService.saveUser(currentUser);

        return false;
    }

    public void updateUserProfile(UserEntity userEntity, ProfileRequest profileRequest) {
        PersonalInformation personalInformation = userEntity.getPersonalInformation();

        if ("on".equals(profileRequest.getPublicPrivateToggle())) {
            userEntity.setPublic(true);
        } else {
            userEntity.setPublic(false);
        }

        personalInformation.setAddress(profileRequest.getAddress());
        personalInformation.setCity(profileRequest.getCity());
        personalInformation.setRegion(profileRequest.getRegion());
        personalInformation.setPostalCode(profileRequest.getPostalCode());
        personalInformation.setFirstName(profileRequest.getSurname());
        personalInformation.setLastName(profileRequest.getName());
        personalInformation.setUser(userEntity);

        userEntity.setPersonalInformation(personalInformation);
        userRepositoryService.saveUser(userEntity);
    }
}
