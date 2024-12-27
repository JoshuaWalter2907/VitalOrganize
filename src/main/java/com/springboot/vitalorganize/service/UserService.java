package com.springboot.vitalorganize.service;

import com.springboot.vitalorganize.dto.ProfileAdditionData;
import com.springboot.vitalorganize.model.*;
import lombok.AllArgsConstructor;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ChatGroupRepository chatGroupRepository;
    private final DirectChatRepository directChatRepository;
    private final MessageRepository messageRepository;


    public String getThemeCss(String theme) {
        return "/css/" + theme + "-theme.css";
    }

    public UserEntity getProfileData(OAuth2User user, OAuth2AuthenticationToken authentication) {
        System.out.println(user);
        System.out.println(authentication);

        // Extrahiere den Authentifizierungs-Provider
        String provider = authentication.getAuthorizedClientRegistrationId();
        String email = user.getAttribute("email");
        String name = user.getAttribute("name");
        String photoUrl;

        if ("github".equals(provider)) {
            String username = user.getAttribute("login");
            email = username + "@github.com"; // Dummy-E-Mail für GitHub erstellen
        }

        UserEntity userEntity = userRepository.findByEmailAndProvider(email, provider);
        if (userEntity == null) {
            throw new IllegalStateException("Benutzer nicht gefunden.");
        }

        photoUrl = userEntity.getProfilePictureUrl();

        // ProfileData-Objekt zurückgeben
        return userEntity;

    }

    public ProfileAdditionData getProfileAdditionData(OAuth2User user, OAuth2AuthenticationToken authentication) {
        String provider = authentication.getAuthorizedClientRegistrationId();
        String email = user.getAttribute("email");

        if ("github".equals(provider)) {
            String username = user.getAttribute("login");
            email = username + "@github.com"; // Dummy-E-Mail erstellen
        }

        UserEntity existingUser = userRepository.findByEmailAndProvider(email, provider);

        boolean isProfileComplete = existingUser != null && !existingUser.getUsername().isEmpty();
        return new ProfileAdditionData(existingUser, isProfileComplete);
    }

    public boolean updateUserProfile(OAuth2User user, OAuth2AuthenticationToken authentication,
                                     String username, boolean isPublic, String birthDate, String mail) {

        String provider = authentication.getAuthorizedClientRegistrationId();
        String email = getEmailFromOAuth2User(user, provider);

        UserEntity existingUser = userRepository.findByEmailAndProvider(email, provider);

//        if(provider.equals("github"))
//            existingUser.setEmail(mail);

        if (existingUser == null) {
            throw new IllegalStateException("Benutzer nicht gefunden");
        }

        // Überprüfen, ob der Benutzername bereits existiert
        if (userRepository.existsByUsername(username)) {
            return true; // Benutzername existiert
        }

        // Benutzerprofil aktualisieren
        existingUser.setSendtoEmail(mail);
        existingUser.setUsername(username);
        existingUser.setPublic(isPublic);
        existingUser.setBirthday(LocalDate.parse(birthDate));

        userRepository.save(existingUser);
        return false; // Benutzername wurde erfolgreich gesetzt
    }

    private String getEmailFromOAuth2User(OAuth2User user, String provider) {
        String email = user.getAttribute("email");

        if ("github".equals(provider)) {
            String username = user.getAttribute("login");
            email = username + "@github.com"; // Dummy-E-Mail erstellen
        }

        return email;
    }

    public UserEntity getCurrentUser(OAuth2User user, OAuth2AuthenticationToken authentication) {
        // Provider bestimmen (z. B. google, discord, github)
        String provider = authentication.getAuthorizedClientRegistrationId();

        // E-Mail und Benutzer-ID ermitteln
        String email = user.getAttribute("email");
        Long id;

        switch (provider) {
            case "google":
            case "discord":
                // Für Google und Discord bleibt die E-Mail unverändert
                id = userRepository.findByEmailAndProvider(email, provider).getId();
                break;

            case "github":
                // Für GitHub muss eine Dummy-E-Mail erstellt werden
                String username = user.getAttribute("login");
                email = username + "@github.com";
                id = userRepository.findByEmailAndProvider(email, provider).getId();
                break;

            default:
                throw new IllegalArgumentException("Unbekannter Provider: " + provider);
        }

        // Benutzer anhand der ID abrufen und zurückgeben
        return userRepository.findUserEntityById(id);
    }


    public UserEntity getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Benutzer nicht gefunden"));
    }

    public List<UserEntity> getPublicUsers() {
        return userRepository.findAllByisPublic(true);
    }

    public Map<Character, List<UserEntity>> getGroupedPublicUsers() {
        List<UserEntity> publicUsers = getPublicUsers();
        return publicUsers.stream()
                .collect(Collectors.groupingBy(user -> user.getUsername().toUpperCase().charAt(0)));
    }

    private String generateRandomPassword() {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()-_=+";
        StringBuilder password = new StringBuilder();
        SecureRandom random = new SecureRandom();

        for (int i = 0; i < 12; i++) { // 12 Zeichen langes Passwort
            int randomIndex = random.nextInt(characters.length());
            password.append(characters.charAt(randomIndex));
        }

        return password.toString();
    }

    @Transactional
    public void deleteUser(UserEntity user) {

        Long id = user.getId();

        List<ChatGroup> chatGroups = chatGroupRepository.findAllByUserId(id);

        List<Long> chatGroupIds = chatGroups.stream()
                .map(ChatGroup::getId)
                .toList();


        messageRepository.deleteByRecipient_Id(id);
        messageRepository.deleteBySender_Id(id);
        chatGroupRepository.deleteAllByIdIn(chatGroupIds);
        directChatRepository.deleteById(id);
        userRepository.deleteById(id);

    }

    @Transactional
    public void blockUser(Long currentUserId, Long targetUserId) {

        System.out.println("Ich war in der blockUsers methode");

        UserEntity currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        UserEntity targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new RuntimeException("User to block not found"));


        if (currentUser.getBlockedUsers().contains(targetUser)) {
            throw new IllegalStateException("User is already blocked");
        }

        // Entferne Freundschaften
        currentUser.getFriends().remove(targetUser);
        targetUser.getFriends().remove(currentUser);

        // Entferne Freundschaftsanfragen
        currentUser.getSentFriendRequests().removeIf(request -> request.getReceiver().equals(targetUser));
        currentUser.getReceivedFriendRequests().removeIf(request -> request.getSender().equals(targetUser));
        targetUser.getSentFriendRequests().removeIf(request -> request.getReceiver().equals(currentUser));
        targetUser.getReceivedFriendRequests().removeIf(request -> request.getSender().equals(currentUser));

        // Füge den Benutzer zur Blockierliste hinzu
        currentUser.getBlockedUsers().add(targetUser);

        // Speichere die Änderungen
        userRepository.save(currentUser);
        userRepository.save(targetUser);
    }

    @Transactional
    public void addFriend(Long currentUserId, Long targetUserId) {
        UserEntity currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        UserEntity targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new RuntimeException("User to add not found"));

        // Überprüfe, ob eine ausstehende Freundschaftsanfrage bereits existiert (aus der Sicht des Empfängers)
        boolean existingRequestReceiver = targetUser.getReceivedFriendRequests().stream()
                .anyMatch(request -> request.getSender().equals(currentUser) && request.getStatus() == FriendRequest.RequestStatus.PENDING);

        // Überprüfe, ob der sendende Benutzer bereits eine ausstehende Anfrage gesendet hat (aus der Sicht des Senders)
        boolean existingRequestSender = currentUser.getSentFriendRequests().stream()
                .anyMatch(request -> request.getReceiver().equals(targetUser) && request.getStatus() == FriendRequest.RequestStatus.PENDING);

        if (existingRequestReceiver || existingRequestSender) {
            // Wenn bereits eine ausstehende Anfrage existiert, tue nichts oder informiere den Benutzer
            return; // Oder zeige eine Nachricht, dass eine Anfrage bereits gesendet wurde
        }

        if (targetUser.isPublic()) {
            // Füge direkt zur Freundesliste hinzu, wenn der Benutzer öffentlich ist
            currentUser.getFriends().add(targetUser);
            targetUser.getFriends().add(currentUser);
        } else {
            System.out.println("Ich habe eine FriendRequest geschickt");
            // Sende eine Freundschaftsanfrage, wenn der Benutzer privat ist
            FriendRequest friendRequest = new FriendRequest();
            friendRequest.setSender(currentUser);
            friendRequest.setReceiver(targetUser);
            friendRequest.setRequestDate(LocalDateTime.now());
            friendRequest.setStatus(FriendRequest.RequestStatus.PENDING); // Setze den Status auf PENDING

            currentUser.getSentFriendRequests().add(friendRequest);
        }

        // Speichere die Änderungen
        userRepository.save(currentUser);
        userRepository.save(targetUser);
    }


    @Transactional
    public void unfriendUser(Long currentUserId, Long targetUserId) {
        UserEntity currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        UserEntity targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new RuntimeException("User to unfriend not found"));

        // Entferne die Freundschaft wechselseitig
        currentUser.getFriends().remove(targetUser);
        targetUser.getFriends().remove(currentUser);

        // Änderungen speichern
        userRepository.save(currentUser);
        userRepository.save(targetUser);
    }

    @Transactional
    public void unblockUser(Long currentUserId, Long targetUserId) {
        UserEntity currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        UserEntity targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new RuntimeException("User to unblock not found"));

        // Entferne den Benutzer aus der Blockierliste
        currentUser.getBlockedUsers().remove(targetUser);

        // Änderungen speichern
        userRepository.save(currentUser);
    }

}