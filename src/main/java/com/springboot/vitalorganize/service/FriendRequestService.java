package com.springboot.vitalorganize.service;

import com.springboot.vitalorganize.entity.Profile_User.FriendRequestEntity;
import com.springboot.vitalorganize.entity.Profile_User.UserEntity;
import com.springboot.vitalorganize.model.Profile.FriendStatusRequestDTO;
import com.springboot.vitalorganize.repository.FriendRequestRepository;
import com.springboot.vitalorganize.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Service zur Verwaltung von Freundschaftsanfragen.
 */
@Service
@AllArgsConstructor
public class FriendRequestService {


    private final FriendRequestRepository FriendRequestRepository;
    private final UserRepository userRepository;
    private final UserService userService;


    public void acceptFriendRequest(FriendStatusRequestDTO friendStatusRequestDTO) {
        UserEntity currentUser = userService.getCurrentUser();
        FriendRequestEntity friendRequest = FriendRequestRepository.findById(friendStatusRequestDTO.getId()).orElse(null);

        assert friendRequest != null;
        friendRequest.setStatus(FriendRequestEntity.RequestStatus.ACCEPTED);
        currentUser.getFriends().add(friendRequest.getSender());
        friendRequest.getSender().getFriends().add(currentUser);

        FriendRequestRepository.save(friendRequest);
        userRepository.save(currentUser);

        FriendRequestRepository.delete(friendRequest);
    }


    public void rejectFriendRequest(FriendStatusRequestDTO friendStatusRequestDTO) {
        FriendRequestEntity friendRequest = FriendRequestRepository.findById(friendStatusRequestDTO.getId()).orElse(null);

        assert friendRequest != null;
        friendRequest.setStatus(FriendRequestEntity.RequestStatus.REJECTED);
        FriendRequestRepository.save(friendRequest);

        FriendRequestRepository.delete(friendRequest);
    }

    public void cancelFriendRequest(FriendStatusRequestDTO friendStatusRequestDTO) {
        UserEntity currentUser = userService.getCurrentUser();
        FriendRequestEntity request = FriendRequestRepository.findById(friendStatusRequestDTO.getId()).orElse(null);

        assert request != null;
        if (!request.getSender().getId().equals(currentUser.getId())) {
            throw new RuntimeException("You are not authorized to cancel this request");
        }

        FriendRequestRepository.delete(request);
    }
}
