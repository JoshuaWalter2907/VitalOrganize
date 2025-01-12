package com.springboot.vitalorganize.service.repositoryhelper;

import com.springboot.vitalorganize.component.PaginationHelper;
import com.springboot.vitalorganize.entity.MessageEntity;
import com.springboot.vitalorganize.entity.UserEntity;
import com.springboot.vitalorganize.repository.MessageRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service-Klasse zur Verwaltung von Nachrichten.
 * Bietet Methoden zum Abrufen, Speichern und Löschen von Nachrichten im Repository.
 * Integriert Pagination und Sortierung für die Nachrichtenabfrage.
 */
@Service
@AllArgsConstructor
public class MessageRepositoryService {

    private final MessageRepository messageRepository;
    private final PaginationHelper paginationHelper;

    /**
     * Sucht Nachrichten zwischen zwei Benutzern, basierend auf den übergebenen IDs.
     * Verwendet die Paginierung, um eine bestimmte Anzahl von Nachrichten auf einmal abzurufen.
     *
     * @param user1Id die ID des ersten Benutzers
     * @param user2Id die ID des zweiten Benutzers
     * @param page die Seitenzahl (für die Paginierung)
     * @param size die Anzahl der Nachrichten pro Seite
     * @return eine Liste von Nachrichten zwischen den beiden Benutzern
     */
    public List<MessageEntity> ChatMessagesBetweenUsers(Long user1Id, Long user2Id, int page, int size){
        // Erstellen eines Pageable-Objekts für die Paginierung, sortiert nach Zeitstempel in absteigender Reihenfolge
        Pageable pageable = paginationHelper.createPageable(page, size, "timestamp", Sort.Direction.DESC);
        // Abrufen der Nachrichten mit der erstellten Paginierung und Rückgabe der Nachrichteninhalte
        return messageRepository.findChatMessages(user1Id, user2Id, pageable).getContent();
    }

    /**
     * Sucht die Teilnehmer eines Chats basierend auf der Benutzer-ID.
     *
     * @param userId die ID des Benutzers
     * @return eine Liste von Benutzern, die an diesem Chat teilnehmen
     */
    public List<UserEntity> ChatParticipants(Long userId){
        return messageRepository.findChatParticipants(userId);
    }

    /**
     * Speichert eine neue Nachricht im Repository.
     *
     * @param messageEntity die zu speichernde Nachricht
     * @return die gespeicherte Nachricht
     */
    public MessageEntity saveMessage(MessageEntity messageEntity){
        return messageRepository.save(messageEntity);
    }

    /**
     * Sucht alle Nachrichten in einem Chat, der einer bestimmten Gruppe zugeordnet ist.
     * Verwendet die Paginierung, um eine bestimmte Anzahl von Nachrichten zu einem Zeitpunkt abzurufen.
     *
     * @param groupId die ID der Chat-Gruppe
     * @param page die Seitenzahl (für die Paginierung)
     * @param size die Anzahl der Nachrichten pro Seite
     * @return eine Liste von Nachrichten in der angegebenen Chat-Gruppe
     */
    public List<MessageEntity> findChatParticipants(Long groupId, int page, int size){
        // Erstellen eines Pageable-Objekts für die Paginierung, sortiert nach Zeitstempel in aufsteigender Reihenfolge
        Pageable pageable = paginationHelper.createPageable(page, size, "timestamp", Sort.Direction.ASC);
        return messageRepository.findByChatGroup_Id(groupId, pageable);
    }

    /**
     * Gibt die letzte Nachricht aus einem Direktchat zurück.
     * Verwendet die Paginierung, um nur die neueste Nachricht zu extrahieren.
     *
     * @param chatId die ID des Direktchats
     * @return die letzte Nachricht im Direktchat
     */
    public MessageEntity getLastDirectChatMessage(Long chatId) {
        // Erstellen eines Pageable-Objekts für eine einzelne Nachricht, sortiert nach Zeitstempel in absteigender Reihenfolge
        Pageable pageable = paginationHelper.createSingleItemPageable("timestamp", Sort.Direction.DESC);
        // Abrufen der letzten Nachricht im Direktchat
        List<MessageEntity> messages = messageRepository.findLastMessageForDirectChat(chatId, pageable);
        // Rückgabe der ersten (und einzigen) Nachricht in der Liste
        return paginationHelper.getFirstElement(messages);
    }

    /**
     * Gibt die letzte Nachricht aus einer Gruppenchat zurück.
     * Verwendet die Paginierung, um nur die neueste Nachricht zu extrahieren.
     *
     * @param groupId die ID der Chat-Gruppe
     * @return die letzte Nachricht im Gruppenchat
     */
    public MessageEntity getLastGroupChatMessage(Long groupId) {
        // Erstellen eines Pageable-Objekts für eine einzelne Nachricht, sortiert nach Zeitstempel in absteigender Reihenfolge
        Pageable pageable = paginationHelper.createSingleItemPageable("timestamp", Sort.Direction.DESC);
        // Abrufen der letzten Nachricht im Gruppenchat
        List<MessageEntity> messages = messageRepository.findLastMessageForChatGroup(groupId, pageable);
        // Rückgabe der ersten (und einzigen) Nachricht in der Liste
        return paginationHelper.getFirstElement(messages);
    }

    /**
     * Löscht alle Nachrichten, bei denen der Empfänger eine bestimmte Benutzer-ID hat.
     *
     * @param id die Benutzer-ID des Empfängers
     */
    public void deleteByRecipient_Id(Long id) {
        messageRepository.deleteByRecipient_Id(id);
    }

    /**
     * Löscht alle Nachrichten, bei denen der Absender eine bestimmte Benutzer-ID hat.
     *
     * @param id die Benutzer-ID des Absenders
     */
    public void deleteBySender_Id(Long id) {
        messageRepository.deleteBySender_Id(id);
    }
}
