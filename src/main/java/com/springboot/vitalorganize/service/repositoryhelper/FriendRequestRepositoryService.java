package com.springboot.vitalorganize.service.repositoryhelper;

import com.springboot.vitalorganize.model.FriendRequest;
import com.springboot.vitalorganize.repository.FriendRequestRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class FriendRequestRepositoryService {

    private final FriendRequestRepository friendRequestRepository;

    public void saveFriendRequest(FriendRequest friendRequest) {
        friendRequestRepository.save(friendRequest);
    }

    public void deleteFriendRequest(FriendRequest friendRequest) {
        friendRequestRepository.delete(friendRequest);
    }

    public FriendRequest findFriendRequestById(Long id) {
        return friendRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Friend request not found"));
    }

    public void deleteById(Long id) {
        friendRequestRepository.deleteById(id);
    }
}
