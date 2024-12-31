package com.springboot.vitalorganize.service.repositoryhelper;

import com.springboot.vitalorganize.model.SubscriptionEntity;
import com.springboot.vitalorganize.repository.SubscriptionRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class SubscriptionRepositoryService {

    private final SubscriptionRepository subscriptionRepository;

    // Methode zum Speichern einer Subscription
    public void saveSubscription(SubscriptionEntity subscriptionEntity) {
        subscriptionRepository.save(subscriptionEntity);
    }
}
