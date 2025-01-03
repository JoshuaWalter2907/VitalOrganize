package com.springboot.vitalorganize.service.repositoryhelper;

import com.springboot.vitalorganize.model.UserEntity;
import com.springboot.vitalorganize.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service-Klasse zur Verwaltung von Benutzerentitäten.
 * Diese Klasse stellt Methoden zum Abrufen, Speichern und Löschen von Benutzerdaten im Repository zur Verfügung.
 */
@Service
@AllArgsConstructor
public class UserRepositoryService {

    private final UserRepository userRepository;

    /**
     * Findet Benutzer anhand einer Liste von Benutzer-IDs.
     *
     * @param userIds Liste von Benutzer-IDs
     * @return eine Liste von Benutzerentitäten
     */
    public List<UserEntity> findUsersByIds(List<Long> userIds) {
        // Findet alle Benutzer im Repository anhand der übergebenen IDs
        return userRepository.findAllById(userIds);
    }

    /**
     * Speichert eine Benutzerentität im Repository.
     *
     * @param userEntity die zu speichernde Benutzerentität
     */
    public void saveUser(UserEntity userEntity) {
        // Speichert den übergebenen Benutzer im Repository
        userRepository.save(userEntity);
    }

    /**
     * Findet einen Benutzer anhand seiner ID.
     *
     * @param id die ID des Benutzers
     * @return den gefundenen Benutzer
     * @throws IllegalArgumentException wenn der Benutzer mit der angegebenen ID nicht gefunden wird
     */
    public UserEntity findUserById(Long id) {
        // Sucht nach dem Benutzer mit der angegebenen ID und wirft eine Ausnahme, wenn dieser nicht existiert
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));
    }

    /**
     * Findet alle Benutzer im Repository.
     *
     * @return eine Liste aller Benutzer
     */
    public List<UserEntity> findAllUsers() {
        // Gibt alle Benutzer aus dem Repository zurück
        return userRepository.findAll();
    }

    /**
     * Findet alle öffentlichen Benutzer, basierend auf der Sichtbarkeit des Profils.
     *
     * @param isPublic ob das Profil öffentlich ist oder nicht
     * @return eine Liste der öffentlichen Benutzer
     */
    public List<UserEntity> findPublicUsers(boolean isPublic) {
        // Sucht alle Benutzer im Repository, die der angegebenen Sichtbarkeit entsprechen
        return userRepository.findAllByisPublic(isPublic);
    }

    /**
     * Findet Benutzer anhand eines Suchbegriffs im Benutzernamen.
     *
     * @param query der Suchbegriff
     * @return eine Liste der Benutzer, deren Benutzernamen den Suchbegriff enthalten
     */
    public List<UserEntity> findByUsernameContaining(String query) {
        // Sucht nach Benutzern, deren Benutzernamen den angegebenen Suchbegriff enthalten (Groß-/Kleinschreibung wird ignoriert)
        return userRepository.findByUsernameContainingIgnoreCase(query);
    }

    /**
     * Findet einen Benutzer anhand seiner E-Mail-Adresse und des Anbieters.
     *
     * @param email    die E-Mail-Adresse des Benutzers
     * @param provider der Anbieter (z. B. "google", "github")
     * @return den gefundenen Benutzer
     */
    public UserEntity findByEmailAndProvider(String email, String provider) {
        // Sucht nach einem Benutzer mit der angegebenen E-Mail und dem Anbieter
        return userRepository.findByEmailAndProvider(email, provider);
    }

    /**
     * Löscht einen Benutzer anhand seiner ID.
     *
     * @param id die ID des zu löschenden Benutzers
     */
    public void deleteById(Long id) {
        // Löscht den Benutzer mit der angegebenen ID aus dem Repository
        userRepository.deleteById(id);
    }

    /**
     * Findet einen Benutzer anhand seines Tokens.
     *
     * @param accessToken das Zugangstoken des Benutzers
     * @return den gefundenen Benutzer
     */
    public UserEntity findByToken(String accessToken) {
        // Sucht nach einem Benutzer mit dem angegebenen Zugangstoken
        return userRepository.findByToken(accessToken);
    }
}
