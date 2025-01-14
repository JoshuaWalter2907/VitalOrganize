package com.springboot.vitalorganize.model.Fund_Payment;

import com.springboot.vitalorganize.entity.Profile_User.UserEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeleteFundRequestDTO {

    private Long fundId;
    private UserEntity loggedInUser;
    private String balance;

}
