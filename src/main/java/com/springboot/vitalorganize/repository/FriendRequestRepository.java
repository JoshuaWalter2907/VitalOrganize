package com.springboot.vitalorganize.repository;

import com.springboot.vitalorganize.entity.Profile_User.FriendRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FriendRequestRepository extends JpaRepository<FriendRequestEntity, Long> {

}
