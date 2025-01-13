package com.springboot.vitalorganize.service;

import com.springboot.vitalorganize.entity.FriendRequest;
import com.springboot.vitalorganize.entity.UserEntity;
import com.springboot.vitalorganize.model.FriendStatusRequestDTO;
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
    private final UserService userService;


    public void acceptFriendRequest(FriendStatusRequestDTO friendStatusRequestDTO) {
        UserEntity currentUser = userService.getCurrentUser();
        FriendRequest friendRequest = friendRequestRepositoryService.findFriendRequestById(friendStatusRequestDTO.getId());

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


    public void rejectFriendRequest(FriendStatusRequestDTO friendStatusRequestDTO) {
        FriendRequest friendRequest = friendRequestRepositoryService.findFriendRequestById(friendStatusRequestDTO.getId());

        // Freundschaftsanfrage ablehnen
        friendRequest.setStatus(FriendRequest.RequestStatus.REJECTED);
        friendRequestRepositoryService.saveFriendRequest(friendRequest);

        // Löschen der abgelehnten Anfrage
        friendRequestRepositoryService.deleteFriendRequest(friendRequest);
    }

    public void cancelFriendRequest(FriendStatusRequestDTO friendStatusRequestDTO) {
        UserEntity currentUser = userService.getCurrentUser();
        FriendRequest request = friendRequestRepositoryService.findFriendRequestById(friendStatusRequestDTO.getId());

        // Überprüfen, ob der aktuelle Benutzer der Absender der Anfrage ist
        if (!request.getSender().getId().equals(currentUser.getId())) {
            throw new RuntimeException("You are not authorized to cancel this request");
        }

        // Anfrage löschen
        friendRequestRepositoryService.deleteFriendRequest(request);
    }
}
