package com.springboot.vitalorganize.service.repositoryhelper;

import com.springboot.vitalorganize.model.FriendRequest;
import com.springboot.vitalorganize.repository.FriendRequestRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Service-Klasse zur Verwaltung und Interaktion mit Freundschaftsanfragen im Repository.
 * Bietet Methoden zum Speichern, Löschen und Abrufen von Freundschaftsanfragen.
 */
@Service
@AllArgsConstructor
public class FriendRequestRepositoryService {

    private final FriendRequestRepository friendRequestRepository;

    /**
     * Speichert eine Freundschaftsanfrage im Repository.
     *
     * @param friendRequest die zu speichernde Freundschaftsanfrage
     */
    public void saveFriendRequest(FriendRequest friendRequest) {
        friendRequestRepository.save(friendRequest);
    }

    /**
     * Löscht eine Freundschaftsanfrage aus dem Repository.
     *
     * @param friendRequest die zu löschende Freundschaftsanfrage
     */
    public void deleteFriendRequest(FriendRequest friendRequest) {
        friendRequestRepository.delete(friendRequest);
    }

    /**
     * Sucht nach einer Freundschaftsanfrage anhand ihrer ID.
     * Wirft eine RuntimeException, wenn die Anfrage nicht gefunden wird.
     *
     * @param id die ID der Freundschaftsanfrage, die gesucht wird
     * @return die Freundschaftsanfrage mit der angegebenen ID
     */
    public FriendRequest findFriendRequestById(Long id) {
        return friendRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Friend request not found"));
    }

    /**
     * Löscht eine Freundschaftsanfrage anhand ihrer ID.
     *
     * @param id die ID der zu löschenden Freundschaftsanfrage
     */
    public void deleteById(Long id) {
        friendRequestRepository.deleteById(id);
    }
}
