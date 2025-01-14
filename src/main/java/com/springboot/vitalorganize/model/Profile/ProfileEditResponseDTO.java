package com.springboot.vitalorganize.model.Profile;

import com.springboot.vitalorganize.entity.Fund_Payments.PaymentEntity;
import com.springboot.vitalorganize.entity.SubscriptionEntity;
import com.springboot.vitalorganize.entity.Fund_Payments.TransactionSubscriptionEntity;
import com.springboot.vitalorganize.entity.Profile_User.UserEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class ProfileEditResponseDTO {

    private String url;
    private List<SubscriptionEntity> subscriptions;
    private UserEntity profile;
    private boolean isProfilePublic;
    private boolean auth;
    private String kind;
    private List<TransactionSubscriptionEntity> historysubscription;
    private List<PaymentEntity> historysingle;
    private String username;
    private String reason;
    private LocalDate datefrom;
    private LocalDate dateto;
    private Long amount;
    private String showSubscription;
    private int totalSubscriptions;
    private int totalSubscriptionPages;
    private int startIndexSubscriptions;
    private int endIndexSubscriptions;
    private int totalPayments;
    private int totalPaymentsPages;
    private int startIndexPayments;
    private int endIndexPayments;
    private int pageSize;
    private int pageNumber;
}
