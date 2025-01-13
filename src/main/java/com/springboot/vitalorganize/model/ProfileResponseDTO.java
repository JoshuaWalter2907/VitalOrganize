package com.springboot.vitalorganize.model;

import com.springboot.vitalorganize.entity.FriendRequest;
import com.springboot.vitalorganize.entity.UserEntity;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ProfileResponseDTO {

    private List<UserEntity> blockedUsers;
    private List<UserEntity> potentialFriends;
    private List<FriendRequest> friendRequests;
    private List<FriendRequest> outgoingFriendRequests;
    private List<UserEntity> friends;
    private UserEntity userEntity;

}
