package com.springboot.vitalorganize.service;

import com.springboot.vitalorganize.model.FriendRequest;
import com.springboot.vitalorganize.model.UserEntity;
import com.springboot.vitalorganize.service.repositoryhelper.FriendRequestRepositoryService;
import com.springboot.vitalorganize.service.repositoryhelper.UserRepositoryService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Service zur Verwaltung von Freundschaftsanfragen.
 * Bietet Methoden zum Akzeptieren, Ablehnen und Abbrechen von Freundschaftsanfragen.
 */
@Service
@AllArgsConstructor
public class FriendRequestService {

    private final FriendRequestRepositoryService friendRequestRepositoryService;
    private final UserRepositoryService userRepositoryService;

    /**
     * Akzeptiert eine Freundschaftsanfrage und fügt den Absender als Freund des aktuellen Benutzers hinzu.
     * Die Anfrage wird anschließend gelöscht.
     *
     * @param requestId die ID der Freundschaftsanfrage
     * @param currentUser der aktuell angemeldete Benutzer, der die Anfrage akzeptiert
     */
    public void acceptFriendRequest(Long requestId, UserEntity currentUser) {
        FriendRequest friendRequest = friendRequestRepositoryService.findFriendRequestById(requestId);

        // Freundschaftsanfrage akzeptieren
        friendRequest.setStatus(FriendRequest.RequestStatus.ACCEPTED);
        currentUser.getFriends().add(friendRequest.getSender());
        friendRequest.getSender().getFriends().add(currentUser);

        // Speichern der aktualisierten Freundschaftsanfrage und Benutzer
        friendRequestRepositoryService.saveFriendRequest(friendRequest);
        userRepositoryService.saveUser(currentUser);

        // Lösche die Anfrage aus der Tabelle, da sie akzeptiert wurde
        friendRequestRepositoryService.deleteFriendRequest(friendRequest);
    }

    /**
     * Lehnt eine Freundschaftsanfrage ab und löscht sie aus der Datenbank.
     *
     * @param requestId die ID der Freundschaftsanfrage
     * @param currentUser der aktuell angemeldete Benutzer, der die Anfrage ablehnt
     */
    public void rejectFriendRequest(Long requestId, UserEntity currentUser) {
        FriendRequest friendRequest = friendRequestRepositoryService.findFriendRequestById(requestId);

        // Freundschaftsanfrage ablehnen
        friendRequest.setStatus(FriendRequest.RequestStatus.REJECTED);
        friendRequestRepositoryService.saveFriendRequest(friendRequest);

        // Löschen der abgelehnten Anfrage
        friendRequestRepositoryService.deleteFriendRequest(friendRequest);
    }

    /**
     * Löscht eine Freundschaftsanfrage, die der aktuelle Benutzer gesendet hat.
     *
     * @param requestId die ID der Freundschaftsanfrage
     * @param currentUser der aktuell angemeldete Benutzer, der die Anfrage gelöscht hat
     * @throws RuntimeException wenn der aktuelle Benutzer nicht der Absender der Anfrage ist
     */
    public void cancelFriendRequest(Long requestId, UserEntity currentUser) {
        FriendRequest request = friendRequestRepositoryService.findFriendRequestById(requestId);

        // Überprüfen, ob der aktuelle Benutzer der Absender der Anfrage ist
        if (!request.getSender().getId().equals(currentUser.getId())) {
            throw new RuntimeException("You are not authorized to cancel this request");
        }

        // Anfrage löschen
        friendRequestRepositoryService.deleteFriendRequest(request);
    }
}
