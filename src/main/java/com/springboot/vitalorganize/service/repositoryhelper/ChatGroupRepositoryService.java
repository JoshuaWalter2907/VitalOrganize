package com.springboot.vitalorganize.service.repositoryhelper;

import com.springboot.vitalorganize.model.ChatGroup;
import com.springboot.vitalorganize.model.UserEntity;
import com.springboot.vitalorganize.repository.ChatGroupRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service-Klasse zur Verwaltung und Interaktion mit Chat-Gruppen im Repository.
 * Bietet Methoden zur Suche, Speicherung und Löschung von Chat-Gruppen sowie zur Durchführung von spezifischen Abfragen.
 */
@Service
@AllArgsConstructor
public class ChatGroupRepositoryService {

    private final ChatGroupRepository chatGroupRepository;

    /**
     * Sucht alle Chat-Gruppen, die den angegebenen Benutzer enthalten.
     *
     * @param userId die ID des Benutzers, nach dessen Chat-Gruppen gesucht werden soll
     * @return eine Liste von Chat-Gruppen, die den Benutzer enthalten
     */
    public List<ChatGroup> findChatGroups(Long userId) {
        return chatGroupRepository.findByUsers_Id(userId);
    }

    /**
     * Sucht nach einer Chat-Gruppe, die die angegebenen Benutzer enthält und einen bestimmten Namen trägt.
     *
     * @param users    die Liste der Benutzer, die in der Chat-Gruppe sein sollen
     * @param chatName der Name der Chat-Gruppe
     * @return eine Optionale Chat-Gruppe, falls eine solche gefunden wurde
     */
    public Optional<ChatGroup> findByUsersInAndName(List<UserEntity> users, String chatName) {
        return chatGroupRepository.findByUsersInAndName(users, chatName);
    }

    /**
     * Speichert eine Chat-Gruppe im Repository.
     *
     * @param chatGroup die zu speichernde Chat-Gruppe
     */
    public void saveChatGroup(ChatGroup chatGroup) {
        chatGroupRepository.save(chatGroup);
    }

    /**
     * Sucht nach einer Chat-Gruppe anhand ihrer ID.
     *
     * @param chatId die ID der Chat-Gruppe, die gesucht wird
     * @return eine Optionale Chat-Gruppe, falls eine solche gefunden wurde
     */
    public Optional<ChatGroup> findById(Long chatId){
        return chatGroupRepository.findById(chatId);
    }

    /**
     * Löscht eine Chat-Gruppe aus dem Repository.
     *
     * @param chat die zu löschende Chat-Gruppe
     */
    public void deleteChatGroup(ChatGroup chat) {
        chatGroupRepository.delete(chat);
    }

    /**
     * Sucht alle Chat-Gruppen, deren Name den angegebenen Suchbegriff enthält.
     *
     * @param query der Suchbegriff, nach dem im Namen der Chat-Gruppen gesucht werden soll
     * @return eine Liste von Chat-Gruppen, deren Name den Suchbegriff enthält
     */
    public List<ChatGroup> findChatGroupsContaining(String query) {
        return chatGroupRepository.findByNameContaining(query);
    }

    /**
     * Gibt alle Chat-Gruppen zurück, die zu einem bestimmten Benutzer gehören.
     *
     * @param id die ID des Benutzers, nach dessen Chat-Gruppen gesucht werden soll
     * @return eine Liste von Chat-Gruppen, die dem Benutzer zugeordnet sind
     */
    public List<ChatGroup> findAllByUserId(Long id) {
        return chatGroupRepository.findAllByUserId(id);
    }

    /**
     * Löscht alle Chat-Gruppen anhand einer Liste von Chat-Gruppen-IDs.
     *
     * @param chatGroupIds die IDs der Chat-Gruppen, die gelöscht werden sollen
     */
    public void deleteAllByIdIn(List<Long> chatGroupIds) {
        chatGroupRepository.deleteAllByIdIn(chatGroupIds);
    }
}
