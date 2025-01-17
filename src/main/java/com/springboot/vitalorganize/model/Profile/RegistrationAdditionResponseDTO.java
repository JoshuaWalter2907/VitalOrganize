package com.springboot.vitalorganize.model.Profile;

import com.springboot.vitalorganize.entity.Profile_User.UserEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegistrationAdditionResponseDTO {

    private String email;
    private String username;
    private String provider;
    private String birthday;
    private UserEntity user;
    private String error;
    private boolean auth;
    private boolean profileComplete = true;

}
