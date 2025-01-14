package com.springboot.vitalorganize.model.Fund_Payment;

import com.springboot.vitalorganize.entity.Fund_Payments.FundEntity;
import com.springboot.vitalorganize.entity.Profile_User.UserEntity;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class EditFundResponseDTO {

    private FundEntity fund;
    private Long id;
    private List<UserEntity> friends;
}
