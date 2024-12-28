package com.springboot.vitalorganize.service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import com.springboot.vitalorganize.dto.ProfileAdditionData;
import com.springboot.vitalorganize.dto.ProfileData;
import com.springboot.vitalorganize.model.PersonalInformation;
import com.springboot.vitalorganize.model.UserEntity;
import com.springboot.vitalorganize.model.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;


    public String getThemeCss(String theme) {
        return "/css/themes/" + theme + ".css";
    }

    public UserEntity getProfileData(OAuth2User user, OAuth2AuthenticationToken authentication) {

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

    public void createPdf(OutputStream out, UserEntity benutzer) throws DocumentException {
        // Neues Dokument erstellen
        Document document = new Document();
        PdfWriter.getInstance(document, out);
        document.open();

        // Benutzerinformationen in PDF einfügen
        document.add(new Paragraph("Benutzerinformationen", new Font(Font.HELVETICA, 16, Font.BOLD)));
        document.add(new Paragraph("Email: " + benutzer.getEmail()));
        document.add(new Paragraph("Nutzername: " + (benutzer.getUsername() != null ? benutzer.getUsername() : "N/A")));
        document.add(new Paragraph("Geburtstag: " + (benutzer.getBirthday() != null ? benutzer.getBirthday().toString() : "N/A")));

        if (benutzer.getPersonalInformation() != null) {
            PersonalInformation personalInfo = benutzer.getPersonalInformation();
            document.add(new Paragraph("\nPersönliche Informationen", new Font(Font.HELVETICA, 14, Font.BOLD)));
            document.add(new Paragraph("Vorname: " + (personalInfo.getFirstName() != null ? personalInfo.getFirstName() : "N/A")));
            document.add(new Paragraph("Nachname: " + (personalInfo.getLastName() != null ? personalInfo.getLastName() : "N/A")));
            document.add(new Paragraph("Adresse: " + (personalInfo.getAddress() != null ? personalInfo.getAddress() : "N/A")));
            document.add(new Paragraph("Postleitzahl: " + (personalInfo.getPostalCode() != null ? personalInfo.getPostalCode() : "N/A")));
            document.add(new Paragraph("Stadt: " + (personalInfo.getCity() != null ? personalInfo.getCity() : "N/A")));
            document.add(new Paragraph("Region: " + (personalInfo.getRegion() != null ? personalInfo.getRegion() : "N/A")));
            document.add(new Paragraph("Land: " + (personalInfo.getCountry() != null ? personalInfo.getCountry() : "N/A")));
        } else {
            document.add(new Paragraph("\nPersönliche Informationen nicht verfügbar."));
        }

        document.close();
    }
}