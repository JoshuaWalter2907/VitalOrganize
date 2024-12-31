package com.springboot.vitalorganize.service;

import com.springboot.vitalorganize.model.FriendRequest;
import com.springboot.vitalorganize.model.UserEntity;
import com.springboot.vitalorganize.service.repositoryhelper.FriendRequestRepositoryService;
import com.springboot.vitalorganize.service.repositoryhelper.UserRepositoryService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class FriendRequestService {

    private final FriendRequestRepositoryService friendRequestRepositoryService;
    private final UserRepositoryService userRepositoryService;

    public void acceptFriendRequest(Long requestId, UserEntity currentUser) {
        FriendRequest friendRequest = friendRequestRepositoryService.findFriendRequestById(requestId);

        // Freundschaftsanfrage akzeptieren
        friendRequest.setStatus(FriendRequest.RequestStatus.ACCEPTED);
        currentUser.getFriends().add(friendRequest.getSender());
        friendRequest.getSender().getFriends().add(currentUser);

        friendRequestRepositoryService.saveFriendRequest(friendRequest);
        userRepositoryService.saveUser(currentUser);

        // Lösche die Anfrage aus der Tabelle, da sie akzeptiert wurde
        friendRequestRepositoryService.deleteFriendRequest(friendRequest);
    }

    public void rejectFriendRequest(Long requestId, UserEntity currentUser) {
        FriendRequest friendRequest = friendRequestRepositoryService.findFriendRequestById(requestId);

        // Freundschaftsanfrage ablehnen
        friendRequest.setStatus(FriendRequest.RequestStatus.REJECTED);
        friendRequestRepositoryService.saveFriendRequest(friendRequest);

        // Löschen der abgelehnten Anfrage
        friendRequestRepositoryService.deleteFriendRequest(friendRequest);
    }

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
