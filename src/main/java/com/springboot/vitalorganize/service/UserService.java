package com.springboot.vitalorganize.service;

import com.springboot.vitalorganize.dto.ProfileAdditionData;
import com.springboot.vitalorganize.model.*;
import com.springboot.vitalorganize.service.repositoryhelper.*;
import lombok.AllArgsConstructor;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
/**
 * Service-Klasse für die Verwaltung von Benutzeroperationen.
 * Diese Klasse bietet Methoden zum Abrufen, Bearbeiten und Löschen von Benutzerdaten,
 * zum Verwalten von Freundschaftsanfragen, Blockierungen und Abonnements.
 */
@Service
@AllArgsConstructor
public class UserService {

    // Abhängigkeiten: Repository-Services für Benutzer, Nachrichten, Chats, Abonnements und Zahlungen
    private final UserRepositoryService userRepositoryService;
    private final MessageRepositoryService messageRepositoryService;
    private final DirektChatRepositoryService direktChatRepositoryService;
    private final ChatGroupRepositoryService chatGroupRepositoryService;
    private final FundRepositoryService fundRepositoryService;
    private final SubscriptionRepositoryService subscriptionRepositoryService;
    private final PaymentRepositoryService paymentRepositoryService;
    private final FriendRequestRepositoryService friendRequestRepositoryService;

    private final PaypalService paypalService;

    /**
     * Ruft die Profildaten des Benutzers basierend auf dem OAuth2User-Objekt ab.
     *
     * @param user Das OAuth2User-Objekt des aktuell angemeldeten Benutzers
     * @param authentication Das OAuth2-Authentifizierungstoken
     * @return Das Benutzerobjekt, das die Profildaten enthält
     */
    public UserEntity getProfileData(OAuth2User user, OAuth2AuthenticationToken authentication) {
        String provider = authentication.getAuthorizedClientRegistrationId();
        String email = user.getAttribute("email");

        // Für GitHub: Dummy-E-Mail-Adresse mit dem GitHub-Benutzernamen erstellen
        if ("github".equals(provider)) {
            String username = user.getAttribute("login");
            email = username + "@github.com";
        }

        // Benutzer anhand der E-Mail und des Anbieters suchen
        UserEntity userEntity = userRepositoryService.findByEmailAndProvider(email, provider);
        if (userEntity == null) {
            throw new IllegalStateException("Benutzer nicht gefunden.");
        }
        return userEntity;
    }

    /**
     * Ruft die Profilerweiterungsdaten ab, z. B. ob das Profil des Benutzers bereits vollständig ist.
     *
     * @param user Das OAuth2User-Objekt des aktuell angemeldeten Benutzers
     * @param authentication Das OAuth2-Authentifizierungstoken
     * @return Ein Objekt mit zusätzlichen Profildaten und einem Status, ob das Profil vollständig ist
     */
    public ProfileAdditionData getProfileAdditionData(OAuth2User user, OAuth2AuthenticationToken authentication) {
        String provider = authentication.getAuthorizedClientRegistrationId();
        String email = user.getAttribute("email");

        // Für GitHub: Dummy-E-Mail-Adresse mit dem GitHub-Benutzernamen erstellen
        if ("github".equals(provider)) {
            String username = user.getAttribute("login");
            email = username + "@github.com";
        }

        UserEntity existingUser = userRepositoryService.findByEmailAndProvider(email, provider);

        boolean isProfileComplete = existingUser != null && !existingUser.getUsername().isEmpty();
        return new ProfileAdditionData(existingUser, isProfileComplete);
    }

    /**
     * Ruft den aktuellen Benutzer basierend auf dem OAuth2User-Objekt und dem Authentifizierungstoken ab.
     *
     * @param user Das OAuth2User-Objekt des aktuell angemeldeten Benutzers
     * @param authentication Das OAuth2-Authentifizierungstoken
     * @return Das Benutzerobjekt des aktuell angemeldeten Benutzers
     */
    public UserEntity getCurrentUser(OAuth2User user, OAuth2AuthenticationToken authentication) {
        String provider = authentication.getAuthorizedClientRegistrationId();
        String email = user.getAttribute("email");
        Long id;

        // Für verschiedene Anbieter (Google, Discord, GitHub) die Benutzer-ID ermitteln
        switch (provider) {
            case "google":
            case "discord":
                id = userRepositoryService.findByEmailAndProvider(email, provider).getId();
                break;
            case "github":
                String username = user.getAttribute("login");
                email = username + "@github.com";
                id = userRepositoryService.findByEmailAndProvider(email, provider).getId();
                break;
            default:
                throw new IllegalArgumentException("Unbekannter Provider: " + provider);
        }

        // Benutzer anhand der ID abrufen und zurückgeben
        return userRepositoryService.findUserById(id);
    }

    /**
     * Ruft einen Benutzer anhand seiner ID ab.
     *
     * @param userId Die ID des Benutzers
     * @return Das Benutzerobjekt für die angegebene ID
     */
    public UserEntity getUserById(Long userId) {
        return userRepositoryService.findUserById(userId);
    }

    /**
     * Ruft alle öffentlichen Benutzer ab.
     *
     * @return Eine Liste öffentlicher Benutzer
     */
    public List<UserEntity> getPublicUsers() {
        return userRepositoryService.findPublicUsers(true);
    }

    /**
     * Löscht einen Benutzer und alle zugehörigen Daten, wenn der Benutzer keine administrativen Aufgaben hat.
     *
     * @param user Das Benutzerobjekt, das gelöscht werden soll
     * @return true, wenn der Benutzer erfolgreich gelöscht wurde, andernfalls false
     */
    @Transactional
    public Boolean deleteUser(UserEntity user) {

        // Überprüft, ob der Benutzer ein Administrator eines Fonds ist
        List<FundEntity> funds = fundRepositoryService.findByAdmin(user);
        if(!funds.isEmpty()) {
            return false;  // Benutzer hat noch administrative Aufgaben
        }

        // Entfernt den Benutzer aus den Mitgliedschaften in Fonds
        List<FundEntity> memberfunds = fundRepositoryService.findALl();
        memberfunds = memberfunds.stream()
                .filter(f -> f.getUsers().contains(user))
                .toList();

        for(FundEntity f : memberfunds) {
            f.getUsers().remove(user);
        }

        Long id = user.getId();

        // Entfernt den Benutzer aus den Chat-Gruppen, Nachrichten und anderen Entitäten
        List<ChatGroup> chatGroups = chatGroupRepositoryService.findAllByUserId(id);
        List<Long> chatGroupIds = chatGroups.stream()
                .map(ChatGroup::getId)
                .toList();

        if(user.getRole().equals("MEMBER"))
            paypalService.cancelSubscription(user, user.getLatestSubscription().getSubscriptionId());

        messageRepositoryService.deleteByRecipient_Id(id);
        messageRepositoryService.deleteBySender_Id(id);
        chatGroupRepositoryService.deleteAllByIdIn(chatGroupIds);
        direktChatRepositoryService.deleteById(id);
        subscriptionRepositoryService.deleteById(id);
        paymentRepositoryService.updateUserReferencesToNull(id);
        friendRequestRepositoryService.deleteById(id);
        userRepositoryService.deleteById(id);

        return true;  // Benutzer erfolgreich gelöscht
    }

    /**
     * Blockiert einen Benutzer und entfernt ihn aus der Freundesliste.
     *
     * @param currentUserId Die ID des aktuellen Benutzers
     * @param targetUserId Die ID des Benutzers, der blockiert werden soll
     */
    @Transactional
    public void blockUser(Long currentUserId, Long targetUserId) {
        UserEntity currentUser = userRepositoryService.findUserById(currentUserId);
        UserEntity targetUser = userRepositoryService.findUserById(targetUserId);

        if (currentUser.getBlockedUsers().contains(targetUser)) {
            throw new IllegalStateException("User is already blocked");
        }

        // Entfernt den Benutzer aus der Freundesliste und den Freundschaftsanfragen
        currentUser.getFriends().remove(targetUser);
        targetUser.getFriends().remove(currentUser);

        currentUser.getSentFriendRequests().removeIf(request -> request.getReceiver().equals(targetUser));
        currentUser.getReceivedFriendRequests().removeIf(request -> request.getSender().equals(targetUser));
        targetUser.getSentFriendRequests().removeIf(request -> request.getReceiver().equals(currentUser));
        targetUser.getReceivedFriendRequests().removeIf(request -> request.getSender().equals(currentUser));

        // Fügt den Benutzer zu den blockierten Benutzern hinzu
        currentUser.getBlockedUsers().add(targetUser);

        userRepositoryService.saveUser(currentUser);
        userRepositoryService.saveUser(targetUser);
    }

    /**
     * Sendet eine Freundschaftsanfrage an einen anderen Benutzer, wenn noch keine Anfrage existiert.
     *
     * @param currentUserId Die ID des aktuellen Benutzers
     * @param targetUserId Die ID des Benutzers, an den die Freundschaftsanfrage gesendet werden soll
     */
    @Transactional
    public void addFriend(Long currentUserId, Long targetUserId) {
        UserEntity currentUser = userRepositoryService.findUserById(currentUserId);
        UserEntity targetUser = userRepositoryService.findUserById(targetUserId);

        // Überprüft, ob bereits eine Freundschaftsanfrage existiert
        boolean existingRequestReceiver = targetUser.getReceivedFriendRequests().stream()
                .anyMatch(request -> request.getSender().equals(currentUser) && request.getStatus() == FriendRequest.RequestStatus.PENDING);

        boolean existingRequestSender = currentUser.getSentFriendRequests().stream()
                .anyMatch(request -> request.getReceiver().equals(targetUser) && request.getStatus() == FriendRequest.RequestStatus.PENDING);

        if (existingRequestReceiver || existingRequestSender) {
            return;  // Keine doppelte Anfrage zulassen
        }

        if (targetUser.isPublic()) {
            // Direkt Freundschaft hinzufügen, wenn das Profil öffentlich ist
            currentUser.getFriends().add(targetUser);
            targetUser.getFriends().add(currentUser);
        } else {
            // Sonst Freundschaftsanfrage senden
            FriendRequest friendRequest = new FriendRequest();
            friendRequest.setSender(currentUser);
            friendRequest.setReceiver(targetUser);
            friendRequest.setRequestDate(LocalDateTime.now());
            friendRequest.setStatus(FriendRequest.RequestStatus.PENDING);

            currentUser.getSentFriendRequests().add(friendRequest);
        }

        userRepositoryService.saveUser(currentUser);
        userRepositoryService.saveUser(targetUser);
    }

    /**
     * Entfernt einen Benutzer aus der Freundesliste.
     *
     * @param currentUserId Die ID des aktuellen Benutzers
     * @param targetUserId Die ID des Benutzers, der aus der Freundesliste entfernt werden soll
     */
    @Transactional
    public void unfriendUser(Long currentUserId, Long targetUserId) {
        UserEntity currentUser = userRepositoryService.findUserById(currentUserId);
        UserEntity targetUser = userRepositoryService.findUserById(targetUserId);

        currentUser.getFriends().remove(targetUser);
        targetUser.getFriends().remove(currentUser);

        userRepositoryService.saveUser(currentUser);
        userRepositoryService.saveUser(targetUser);
    }

    /**
     * Entblockiert einen zuvor blockierten Benutzer.
     *
     * @param currentUserId Die ID des aktuellen Benutzers
     * @param targetUserId Die ID des Benutzers, der entblockiert werden soll
     */
    @Transactional
    public void unblockUser(Long currentUserId, Long targetUserId) {
        UserEntity currentUser = userRepositoryService.findUserById(currentUserId);
        UserEntity targetUser = userRepositoryService.findUserById(targetUserId);

        currentUser.getBlockedUsers().remove(targetUser);

        userRepositoryService.saveUser(currentUser);
    }

    /**
     * Ruft eine Liste von Benutzern ab, die entweder öffentlich sind oder mit dem aktuellen Benutzer befreundet sind.
     *
     * @param userId Die ID des aktuellen Benutzers
     * @return Eine Liste der öffentlichen Benutzer oder Freunde des aktuellen Benutzers
     */
    public List<UserEntity> getUsersWithFriendsOrPublic(Long userId) {
        UserEntity currentUser = userRepositoryService.findUserById(userId);

        // Öffentliche Benutzer und Freunde des Benutzers abrufen
        List<UserEntity> publicUsers = userRepositoryService.findPublicUsers(true);
        List<UserEntity> friends = currentUser.getFriends();

        publicUsers.addAll(friends);

        // Rückgabe der kombinierten Liste ohne Duplikate
        return publicUsers.stream().distinct().collect(Collectors.toList());
    }

    /**
     * Sucht Benutzer nach ihrem Benutzernamen.
     *
     * @param query Der Suchbegriff für den Benutzernamen
     * @return Eine Liste der Benutzer, deren Benutzernamen den Suchbegriff enthalten
     */
    public List<UserEntity> findByUsername(String query){
        return userRepositoryService.findByUsernameContaining(query);
    }

    /**
     * Ruft die E-Mail-Adresse des Benutzers basierend auf dem OAuth2Provider ab.
     *
     * @param user Das OAuth2User-Objekt des aktuell angemeldeten Benutzers
     * @param provider Der Name des Anbieters (z.B. GitHub)
     * @return Die E-Mail-Adresse des Benutzers
     */
    public String getEmailForUser(OAuth2User user, String provider) {
        if ("github".equals(provider)) {
            String username = user.getAttribute("login");
            return username + "@github.com";  // Dummy-E-Mail für GitHub
        }
        return user.getAttribute("email");
    }

    public void togglePriceReportEmail(Long userId) {
        userRepositoryService.togglePriceReportsEnabled(userId);
    }


}
