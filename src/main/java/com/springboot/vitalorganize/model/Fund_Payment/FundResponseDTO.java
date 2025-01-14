package com.springboot.vitalorganize.model.Fund_Payment;

import com.springboot.vitalorganize.entity.Fund_Payments.FundEntity;
import com.springboot.vitalorganize.entity.Fund_Payments.PaymentEntity;
import com.springboot.vitalorganize.entity.Profile_User.UserEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class FundResponseDTO {

    private UserEntity loggedInUser;
    private List<FundEntity> funds;
    private FundEntity myfunds;
    private List<PaymentEntity> fundpayments;
    private double balance;
    private boolean show;
    private boolean error;
    private int totalPayments;
    private int pageNumber;
    private int pageSize;
    private int totalPages;
    private String username;
    private String reason;
    private LocalDate datefrom;
    private LocalDate dateto;
    private Long amount;

}
