package com.springboot.vitalorganize.service;

import com.springboot.vitalorganize.model.ProfileRequest;
import com.springboot.vitalorganize.entity.*;
import com.springboot.vitalorganize.service.repositoryhelper.PaymentRepositoryService;
import com.springboot.vitalorganize.service.repositoryhelper.UserRepositoryService;
import lombok.AllArgsConstructor;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service-Klasse zur Handhabung von Benutzerprofilen.
 * Diese Klasse bietet Methoden zur Verwaltung und Anzeige von Benutzerprofilen,
 * zur Verwaltung von Freundschaftsanfragen, Blockierungen sowie Abonnements und Zahlungsverläufen.
 */
@Service
@AllArgsConstructor
public class ProfileService {

    private final UserRepositoryService userRepositoryService;
    private final UserService userService;
    private final PaypalService paypalService;
    private final PaymentRepositoryService paymentRepositoryService;

    /**
     * Holt das Benutzerprofil des aktuellen Benutzers oder eines angegebenen Benutzers.
     *
     * @param user               Das OAuth2User-Objekt des aktuellen Benutzers
     * @param authenticationToken Das OAuth2AuthenticationToken des aktuellen Benutzers
     * @param profileId          Die ID des angeforderten Benutzerprofils
     * @return Das Benutzerprofil des aktuellen Benutzers oder eines anderen Benutzers
     */
    public UserEntity getProfileUser(OAuth2User user, OAuth2AuthenticationToken authenticationToken, Long profileId) {
        UserEntity currentUser = userService.getCurrentUser(user, authenticationToken);

        // Überprüft, ob das Profil des aktuellen Benutzers angezeigt wird
        if (profileId != null && profileId.equals(currentUser.getId())) {
            return currentUser; // Gibt das Profil des aktuellen Benutzers zurück
        }

        // Holt den Benutzer mit der angegebenen Profil-ID
        return userRepositoryService.findUserById(profileId);
    }

    /**
     * Holt eine Liste potentieller Freunde für den aktuellen Benutzer.
     * Potentielle Freunde sind Benutzer, die noch nicht blockiert oder befreundet sind
     * und denen keine offene Freundschaftsanfrage gesendet wurde.
     *
     * @param currentUser Das Benutzerobjekt des aktuellen Benutzers
     * @return Eine Liste potentieller Freunde
     */
    public List<UserEntity> getPotentialFriends(UserEntity currentUser) {
        List<UserEntity> blockedUsers = currentUser.getBlockedUsers();
        List<UserEntity> allUsers = userRepositoryService.findAllUsers(); // Holt alle Benutzer

        return allUsers.stream()
                .filter(u -> !u.equals(currentUser)) // Schließt den aktuellen Benutzer aus
                .filter(u -> !currentUser.getFriends().contains(u)) // Schließt bereits befreundete Benutzer aus
                .filter(u -> !blockedUsers.contains(u)) // Schließt blockierte Benutzer aus
                .filter(u -> !u.getBlockedUsers().contains(currentUser)) // Schließt Benutzer aus, die den aktuellen Benutzer blockiert haben
                .filter(u -> currentUser.getSentFriendRequests().stream()
                        .noneMatch(fr -> fr.getReceiver().equals(u) && fr.getStatus() == FriendRequest.RequestStatus.PENDING)) // Schließt Benutzer aus, denen bereits eine offene Anfrage gesendet wurde
                .collect(Collectors.toList());
    }

    /**
     * Holt die Freundschaftsanfragen, die der Benutzer empfangen hat.
     *
     * @param currentUser Das Benutzerobjekt des aktuellen Benutzers
     * @return Eine Liste von Freundschaftsanfragen
     */
    public List<FriendRequest> getFriendRequests(UserEntity currentUser) {
        return currentUser.getReceivedFriendRequests();
    }

    /**
     * Holt die Freundschaftsanfragen, die der Benutzer gesendet hat und noch offen sind.
     *
     * @param currentUser Das Benutzerobjekt des aktuellen Benutzers
     * @return Eine Liste offener gesendeter Freundschaftsanfragen
     */
    public List<FriendRequest> getSentRequests(UserEntity currentUser) {
        return currentUser.getSentFriendRequests().stream()
                .filter(fr -> fr.getStatus() == FriendRequest.RequestStatus.PENDING)
                .collect(Collectors.toList());
    }

    /**
     * Holt die blockierten Benutzer des aktuellen Benutzers.
     *
     * @param currentUser Das Benutzerobjekt des aktuellen Benutzers
     * @return Eine Liste blockierter Benutzer
     */
    public List<UserEntity> getBlockedUsers(UserEntity currentUser) {
        return currentUser.getBlockedUsers();
    }

    /**
     * Holt die Profildaten eines Benutzers basierend auf der angegebenen Profil-ID.
     * Wenn keine ID angegeben ist, werden die Profildaten des aktuell angemeldeten Benutzers abgerufen.
     *
     * @param profileId        Die ID des angeforderten Benutzerprofils
     * @param user             Das OAuth2User-Objekt des aktuellen Benutzers
     * @param authentication   Das OAuth2AuthenticationToken des aktuellen Benutzers
     * @return Das Benutzerobjekt des angeforderten Profils
     */
    public UserEntity getProfileData(Long profileId, OAuth2User user, OAuth2AuthenticationToken authentication) {
        UserEntity profileData;
        if (profileId == null) {
            profileData = userService.getProfileData(user, authentication);
        } else {
            profileData = userService.getUserById(profileId);
        }

        return profileData;
    }

    /**
     * Holt eine Liste der Abonnements eines Benutzers und kehrt die Reihenfolge um.
     *
     * @param profileData Das Benutzerobjekt des angeforderten Profils
     * @return Eine Liste der Abonnements
     */
    public List<SubscriptionEntity> getSubscriptions(UserEntity profileData) {
        List<SubscriptionEntity> subscriptions = profileData.getSubscriptions();
        Collections.reverse(subscriptions);  // Die Reihenfolge umkehren
        return subscriptions;
    }

    /**
     * Holt die Transaktionshistorie für das Abonnement eines Benutzers, wenn das Abonnement existiert.
     * Filtert die Transaktionen nach den angegebenen Kriterien wie Username, Datum und Betrag.
     *
     * @param profileData Das Benutzerobjekt des angeforderten Profils
     * @param kind        Der Abonnementtyp
     * @param username    Der Benutzername, nach dem gefiltert wird
     * @param datefrom    Das Startdatum für die Transaktionen
     * @param dateto      Das Enddatum für die Transaktionen
     * @param amount      Der Betrag, nach dem gefiltert wird
     * @return Eine Liste von Transaktionen oder null, wenn keine Transaktionen vorhanden sind
     */
    public List<TransactionSubscription> getTransactionHistory(UserEntity profileData, String kind, String username, LocalDate datefrom, LocalDate dateto, Long amount) {
        if ("premium".equals(kind) && profileData.getLatestSubscription() != null) {
            List<TransactionSubscription> transactions = paypalService.getTransactionsForSubscription(
                    profileData.getLatestSubscription().getSubscriptionId()
            );
            return paypalService.filterTransactions(transactions, username, datefrom, dateto, amount);
        }
        return null;  // Alternativ: kann auch eine Liste von Zahlungen zurückgeben
    }

    /**
     * Holt gefilterte Zahlungen basierend auf den angegebenen Kriterien (Username, Grund, Datum, Betrag).
     *
     * @param profileData Das Benutzerobjekt des angeforderten Profils
     * @param username    Der Benutzername, nach dem gefiltert wird
     * @param reason      Der Grund der Zahlung
     * @param datefrom    Das Startdatum für die Zahlungen
     * @param dateto      Das Enddatum für die Zahlungen
     * @param amount      Der Betrag, nach dem gefiltert wird
     * @return Eine Liste gefilterter Zahlungen
     */
    public List<Payment> getFilteredPayments(UserEntity profileData, String username, String reason, LocalDate datefrom, LocalDate dateto, Long amount) {
        List<Payment> payments = paymentRepositoryService.findPaymentsByUser(profileData);
        return paypalService.filterPayments(payments, username, reason, datefrom, dateto, amount);
    }

    /**
     * Bestimmt den aktiven Tab basierend auf dem übergebenen Tab-Namen.
     *
     * @param tab Der Name des Tabs
     * @return Der Name des aktiven Tabs
     */
    public String determineTab(String tab) {
        // Logik zur Bestimmung des aktiven Tabs
        if ("subscription".equals(tab)) {
            return "subscription";
        } else if ("paymenthistory".equals(tab)) {
            return "paymenthistory";
        }
        return "general"; // Standardwert
    }

    /**
     * Aktualisiert das Profil des Benutzers (Benutzername, Geburtsdatum, E-Mail).
     * Prüft, ob der Benutzername bereits existiert.
     *
     * @param user           Das OAuth2User-Objekt des aktuellen Benutzers
     * @param authentication Das OAuth2AuthenticationToken des aktuellen Benutzers
     * @param username       Der neue Benutzername
     * @param birthdate      Das neue Geburtsdatum im String-Format
     * @param email          Die neue E-Mail-Adresse
     * @return true, wenn der Benutzername bereits existiert, andernfalls false
     */
    public boolean updateUserProfile(OAuth2User user, OAuth2AuthenticationToken authentication, String username, String birthdate, String email) {
        // Benutzerdaten abrufen
        UserEntity currentUser = userService.getCurrentUser(user, authentication);

        // Überprüfen, ob der Benutzername bereits existiert
        if (userRepositoryService.findAllUsers().stream()
                .anyMatch(u -> u.getUsername() != null && u.getUsername().equals(username))) {
            return true;  // Benutzername existiert bereits
        }

        // Profil aktualisieren
        currentUser.setUsername(username);
        currentUser.setBirthday(LocalDate.parse(birthdate));  // Beispiel: Geburtsdatum setzen
        if(!email.isBlank())
            currentUser.setSendtoEmail(email);
        userRepositoryService.saveUser(currentUser);

        return false;
    }

    /**
     * Aktualisiert das Profil eines Benutzers mit den Daten aus der ProfileRequest.
     *
     * @param userEntity     Das Benutzerobjekt, dessen Profil aktualisiert wird
     * @param profileRequest Das Profil-Request-Objekt mit den neuen Daten
     */
    public void updateUserProfile(UserEntity userEntity, ProfileRequest profileRequest) {
        PersonalInformation personalInformation = userEntity.getPersonalInformation();

        System.out.println(profileRequest.getPublicPrivateToggle());

        userEntity.setPublic("on".equals(profileRequest.getPublicPrivateToggle()));  // Setzt das öffentliche Profil

        // Setzt die persönlichen Informationen des Benutzers
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
