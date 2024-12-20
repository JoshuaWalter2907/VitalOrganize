package com.springboot.vitalorganize.service;

import com.springboot.vitalorganize.dto.ProfileAdditionData;
import com.springboot.vitalorganize.dto.ProfileData;
import com.springboot.vitalorganize.model.UserEntity;
import com.springboot.vitalorganize.model.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;


    public String getThemeCss(String theme) {
        return "/css/" + theme + "-theme.css";
    }

    public ProfileData getProfileData(OAuth2User user, OAuth2AuthenticationToken authentication) {
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
        return new ProfileData(name, email, photoUrl);

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
                                     String username, boolean isPublic, String birthDate) {

        String provider = authentication.getAuthorizedClientRegistrationId();
        String email = getEmailFromOAuth2User(user, provider);

        UserEntity existingUser = userRepository.findByEmailAndProvider(email, provider);

        if (existingUser == null) {
            throw new IllegalStateException("Benutzer nicht gefunden");
        }

        // Überprüfen, ob der Benutzername bereits existiert
        if (userRepository.existsByUsername(username)) {
            return true; // Benutzername existiert
        }

        // Benutzerprofil aktualisieren
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
}