package com.springboot.vitalorganize.model;

import com.springboot.vitalorganize.entity.Payment;
import com.springboot.vitalorganize.entity.SubscriptionEntity;
import com.springboot.vitalorganize.entity.TransactionSubscription;
import com.springboot.vitalorganize.entity.UserEntity;
import lombok.Getter;
import lombok.Setter;
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
    private List<TransactionSubscription> historysubscription;
    private List<Payment> historysingle;
    private String showSubscription;
}
