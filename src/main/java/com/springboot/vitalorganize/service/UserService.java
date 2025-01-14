package com.springboot.vitalorganize.service;

import com.springboot.vitalorganize.entity.Chat.ChatGroupEntity;
import com.springboot.vitalorganize.entity.Fund_Payments.FundEntity;
import com.springboot.vitalorganize.entity.Fund_Payments.PaymentEntity;
import com.springboot.vitalorganize.entity.Profile_User.FriendRequestEntity;
import com.springboot.vitalorganize.entity.Profile_User.UserEntity;
import com.springboot.vitalorganize.model.Profile.FriendStatusRequestDTO;
import com.springboot.vitalorganize.repository.*;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
/**
 * Service-Klasse für die Verwaltung von Benutzeroperationen
 */
@Service
@AllArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final DirectChatRepository directChatRepository;
    private final ChatGroupRepository chatGroupRepository;
    private final FundRepository fundRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PaymentRepository paymentRepository;
    private final FriendRequestRepository friendRequestRepository;

    private final PaypalService paypalService;


    /**
     * Ruft den aktuellen Benutzer basierend auf dem OAuth2User-Objekt und dem Authentifizierungstoken ab.
     * @param user Das OAuth2User-Objekt des aktuell angemeldeten Benutzers
     * @param authentication Das OAuth2-Authentifizierungstoken
     * @return Das Benutzerobjekt des aktuell angemeldeten Benutzers
     */
    public UserEntity getCurrentUser(OAuth2User user, OAuth2AuthenticationToken authentication) {
        String provider = authentication.getAuthorizedClientRegistrationId();
        String email = user.getAttribute("email");
        Long id;

        switch (provider) {
            case "google":
            case "discord":
                id = userRepository.findByEmailAndProvider(email, provider).getId();
                break;
            case "github":
                String username = user.getAttribute("login");
                email = username + "@github.com";
                id = userRepository.findByEmailAndProvider(email, provider).getId();
                break;
            default:
                throw new IllegalArgumentException("Unbekannter Provider: " + provider);
        }

        return userRepository.findUserEntityById(id);
    }

    /**
     * Holt den aktuellen Nutzer
     * @return der Benutzer
     */
    public UserEntity getCurrentUser() {
        if(SecurityContextHolder.getContext().getAuthentication() instanceof AnonymousAuthenticationToken) {
            return null;
        }

        OAuth2AuthenticationToken authentication =
                (OAuth2AuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        OAuth2User user = authentication.getPrincipal();

        String provider = authentication.getAuthorizedClientRegistrationId();
        String email = user.getAttribute("email");
        Long id;

        switch (provider) {
            case "google":
            case "discord":
                id = userRepository.findByEmailAndProvider(email, provider).getId();
                break;
            case "github":
                String username = user.getAttribute("login");
                email = username + "@github.com";
                id = userRepository.findByEmailAndProvider(email, provider).getId();
                break;
            default:
                throw new IllegalArgumentException("Unbekannter Provider: " + provider);
        }

        return userRepository.findUserEntityById(id);
    }



    /**
     * Ruft einen Benutzer anhand seiner ID ab.
     * @param userId Die ID des Benutzers
     * @return Das Benutzerobjekt für die angegebene ID
     */
    public UserEntity getUserById(Long userId) {
        return userRepository.findUserEntityById(userId);
    }

    /**
     * Ruft alle öffentlichen Benutzer ab.
     * @return Eine Liste öffentlicher Benutzer
     */
    public List<UserEntity> getPublicUsers() {
        return userRepository.findAllByisPublic(true);
    }

    /**
     * Löscht einen Benutzer und alle zugehörigen Daten, wenn der Benutzer keine administrativen Aufgaben hat.
     * @return true, wenn der Benutzer erfolgreich gelöscht wurde, andernfalls false
     */
    @Transactional
    public Boolean deleteUser() {
        UserEntity user = getCurrentUser();

        List<FundEntity> funds = fundRepository.findByAdmin(user);
        if(!funds.isEmpty()) {
            return false;
        }

        List<FundEntity> memberfunds = fundRepository.findAll();
        memberfunds = memberfunds.stream()
                .filter(f -> f.getUsers().contains(user))
                .toList();

        for(FundEntity f : memberfunds) {
            f.getUsers().remove(user);
        }

        Long id = user.getId();

        List<ChatGroupEntity> chatGroups = chatGroupRepository.findByUsers_Id(id);
        List<Long> chatGroupIds = chatGroups.stream()
                .map(ChatGroupEntity::getId)
                .toList();

        if(user.getRole().equals("MEMBER"))
            paypalService.cancelSubscription(user, user.getLatestSubscription().getSubscriptionId());

        messageRepository.deleteByRecipient_Id(id);
        messageRepository.deleteBySender_Id(id);
        chatGroupRepository.deleteAllByIdIn(chatGroupIds);
        directChatRepository.deleteById(id);
        friendRequestRepository.deleteById(id);
        userRepository.deleteById(id);
        List<PaymentEntity> payments = paymentRepository.findByUserId(id);

        // Setze das 'user' Feld auf null
        for (PaymentEntity payment : payments) {
            payment.setUser(null);
        }

        // Speichere alle geänderten Zahlungen
        paymentRepository.saveAll(payments);

        return true;
    }


    /**
     * blockiert einen Nutzer
     * @param friendRequestDTO Die benötigten Informationen
     */
    public void blockUser(FriendStatusRequestDTO friendRequestDTO) {
        UserEntity currentUser = userRepository.findUserEntityById(getCurrentUser().getId());
        UserEntity targetUser = userRepository.findUserEntityById(friendRequestDTO.getId());

        if (currentUser.getBlockedUsers().contains(targetUser)) {
            throw new IllegalStateException("User is already blocked");
        }

        currentUser.getFriends().remove(targetUser);
        targetUser.getFriends().remove(currentUser);

        currentUser.getSentFriendRequests().removeIf(request -> request.getReceiver().equals(targetUser));
        currentUser.getReceivedFriendRequests().removeIf(request -> request.getSender().equals(targetUser));
        targetUser.getSentFriendRequests().removeIf(request -> request.getReceiver().equals(currentUser));
        targetUser.getReceivedFriendRequests().removeIf(request -> request.getSender().equals(currentUser));

        currentUser.getBlockedUsers().add(targetUser);

        userRepository.save(currentUser);
        userRepository.save(targetUser);
    }


    /**
     * Fügt einen neuen Freund hinzu oder sendet eine Freundschaftsanfrage
     * @param friendStatusRequestDTO Benötigte Informationen
     */
    public void addFriend(FriendStatusRequestDTO friendStatusRequestDTO) {
        UserEntity currentUser = userRepository.findUserEntityById(getCurrentUser().getId());
        UserEntity targetUser = userRepository.findUserEntityById(friendStatusRequestDTO.getId());

        boolean existingRequestReceiver = targetUser.getReceivedFriendRequests().stream()
                .anyMatch(request -> request.getSender().equals(currentUser) && request.getStatus() == FriendRequestEntity.RequestStatus.PENDING);

        boolean existingRequestSender = currentUser.getSentFriendRequests().stream()
                .anyMatch(request -> request.getReceiver().equals(targetUser) && request.getStatus() == FriendRequestEntity.RequestStatus.PENDING);

        if (existingRequestReceiver || existingRequestSender) {
            return;
        }

        if (targetUser.isPublic()) {
            currentUser.getFriends().add(targetUser);
            targetUser.getFriends().add(currentUser);
        } else {
            FriendRequestEntity friendRequest = new FriendRequestEntity();
            friendRequest.setSender(currentUser);
            friendRequest.setReceiver(targetUser);
            friendRequest.setRequestDate(LocalDateTime.now());
            friendRequest.setStatus(FriendRequestEntity.RequestStatus.PENDING);

            currentUser.getSentFriendRequests().add(friendRequest);
        }

        userRepository.save(currentUser);
        userRepository.save(targetUser);
    }


    /**
     * Löscht eine Freunschaft
     * @param friendStatusRequestDTO benötigte Informationen
     */
    public void unfriendUser(FriendStatusRequestDTO friendStatusRequestDTO) {
        UserEntity currentUser = userRepository.findUserEntityById(getCurrentUser().getId());
        UserEntity targetUser = userRepository.findUserEntityById(friendStatusRequestDTO.getId());

        currentUser.getFriends().remove(targetUser);
        targetUser.getFriends().remove(currentUser);

        userRepository.save(currentUser);
        userRepository.save(targetUser);
    }


    /**
     * Löscht die Blockierung eines Nutzers
     * @param friendStatusRequestDTO benötigte Informationen
     */
    public void unblockUser(FriendStatusRequestDTO friendStatusRequestDTO) {
        UserEntity currentUser = userRepository.findUserEntityById(getCurrentUser().getId());
        UserEntity targetUser = userRepository.findUserEntityById(friendStatusRequestDTO.getId());

        currentUser.getBlockedUsers().remove(targetUser);

        userRepository.save(currentUser);
    }

    /**
     * Ruft eine Liste von Benutzern ab, die entweder öffentlich sind oder mit dem aktuellen Benutzer befreundet sind.
     * @param userId Die ID des aktuellen Benutzers
     * @return Eine Liste der öffentlichen Benutzer oder Freunde des aktuellen Benutzers
     */
    public List<UserEntity> getUsersWithFriendsOrPublic(Long userId) {
        UserEntity currentUser = userRepository.findUserEntityById(userId);

        // Öffentliche Benutzer und Freunde des Benutzers abrufen
        List<UserEntity> publicUsers = userRepository.findAllByisPublic(true);
        List<UserEntity> friends = currentUser.getFriends();

        publicUsers.addAll(friends);

        // Rückgabe der kombinierten Liste ohne Duplikate
        return publicUsers.stream().distinct().collect(Collectors.toList());
    }

    /**
     * Sucht Benutzer nach ihrem Benutzernamen.
     * @param query Der Suchbegriff für den Benutzernamen
     * @return Eine Liste der Benutzer, deren Benutzernamen den Suchbegriff enthalten
     */
    public List<UserEntity> findByUsername(String query){
        return userRepository.findByUsernameContainingIgnoreCase(query);
    }


    public void togglePriceReportEmail(Long userId) {
        UserEntity userEntity = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        userEntity.setPriceReportsEnabled(!userEntity.isPriceReportsEnabled());
        userRepository.save(userEntity);
    }


    public UserEntity getUser(Long id) {
        if (id == null)
            return getCurrentUser();
        return userRepository.findUserEntityById(id);
    }
}
