package com.springboot.vitalorganize.service.repositoryhelper;

import com.springboot.vitalorganize.entity.DirectChat;
import com.springboot.vitalorganize.repository.DirectChatRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service-Klasse zur Verwaltung und Interaktion mit Direktnachrichten (Direct Chat) im Repository.
 * Bietet Methoden zur Suche, Speicherung und Löschung von Direktnachrichten zwischen Benutzern.
 */
@Service
@AllArgsConstructor
public class DirektChatRepositoryService {

    private final DirectChatRepository directChatRepository;

    /**
     * Sucht nach einer Direktnachricht zwischen zwei Benutzern anhand ihrer IDs.
     * Diese Methode berücksichtigt beide Richtungen der Kommunikation (user1 -> user2 und user2 -> user1).
     *
     * @param user1Id die ID des ersten Benutzers
     * @param user2Id die ID des zweiten Benutzers
     * @return die Direktnachricht zwischen den beiden Benutzern, falls vorhanden
     */
    public DirectChat findDirectChatBetweenUsers(Long user1Id, Long user2Id) {
        DirectChat directChat = directChatRepository.findByUser1IdAndUser2Id(user1Id, user2Id);
        // Wenn die Direktnachricht nicht gefunden wurde, wird die Richtung vertauscht gesucht
        if (directChat == null) {
            directChat = directChatRepository.findByUser2IdAndUser1Id(user1Id, user2Id);
        }
        return directChat;
    }

    /**
     * Sucht alle Direktnachrichten, die von einem Benutzer (user1Id) gesendet wurden oder empfangen wurden.
     *
     * @param user1Id die ID des Benutzers, nach dessen Direktnachrichten gesucht werden soll
     * @return eine Liste von Direktnachrichten, die den Benutzer enthalten
     */
    public List<DirectChat> findDirectChats(Long user1Id) {
        return directChatRepository.findByUser1IdOrUser2Id(user1Id, user1Id);
    }

    /**
     * Speichert eine Direktnachricht im Repository.
     *
     * @param directChat die zu speichernde Direktnachricht
     */
    public void saveDirectChat(DirectChat directChat) {
        directChatRepository.save(directChat);
    }

    /**
     * Sucht nach einer Direktnachricht anhand ihrer ID.
     *
     * @param chatId die ID der Direktnachricht, die gesucht wird
     * @return eine Optionale Direktnachricht, falls eine solche gefunden wurde
     */
    public Optional<DirectChat> findById(Long chatId){
        return directChatRepository.findById(chatId);
    }

    /**
     * Löscht eine Direktnachricht aus dem Repository.
     *
     * @param chat die zu löschende Direktnachricht
     */
    public void deleteDirectChat(DirectChat chat) {
        directChatRepository.delete(chat);
    }

    /**
     * Löscht eine Direktnachricht anhand ihrer ID.
     *
     * @param id die ID der zu löschenden Direktnachricht
     */
    public void deleteById(Long id) {
        directChatRepository.deleteById(id);
    }
}
