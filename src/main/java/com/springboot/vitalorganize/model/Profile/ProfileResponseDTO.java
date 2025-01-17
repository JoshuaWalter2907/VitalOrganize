package com.springboot.vitalorganize.model.Profile;

import com.springboot.vitalorganize.entity.Profile_User.FriendRequestEntity;
import com.springboot.vitalorganize.entity.Profile_User.UserEntity;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ProfileResponseDTO {

    private List<UserEntity> blockedUsers;
    private List<UserEntity> potentialFriends;
    private List<FriendRequestEntity> friendRequests;
    private List<FriendRequestEntity> outgoingFriendRequests;
    private List<UserEntity> friends;
    private UserEntity userEntity;

}
