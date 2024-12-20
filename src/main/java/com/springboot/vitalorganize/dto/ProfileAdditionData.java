package com.springboot.vitalorganize.dto;

import com.springboot.vitalorganize.model.UserEntity;
import lombok.Getter;

import java.time.LocalDate;

public class ProfileAdditionData {

    @Getter
    private final UserEntity userEntity;
    private final boolean isProfileComplete;

    public ProfileAdditionData(UserEntity userEntity, boolean isProfileComplete) {
        this.userEntity = userEntity;
        this.isProfileComplete = isProfileComplete;
    }

    public boolean isProfileComplete() {
        return isProfileComplete;
    }

    public LocalDate getBirthDate() {
        return userEntity != null ? userEntity.getBirthday() : null;
    }
}
