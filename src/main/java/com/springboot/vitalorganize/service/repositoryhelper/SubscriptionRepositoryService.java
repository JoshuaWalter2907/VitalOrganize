package com.springboot.vitalorganize.service.repositoryhelper;

import com.springboot.vitalorganize.entity.SubscriptionEntity;
import com.springboot.vitalorganize.repository.SubscriptionRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Service-Klasse zur Verwaltung von Subscription-Entitäten.
 * Diese Klasse bietet Methoden zum Speichern und Löschen von Subscription-Daten in der Repository.
 */
@Service
@AllArgsConstructor
public class SubscriptionRepositoryService {

    private final SubscriptionRepository subscriptionRepository;

    /**
     * Speichert eine Subscription im Repository.
     *
     * @param subscriptionEntity die zu speichernde Subscription
     */
    public void saveSubscription(SubscriptionEntity subscriptionEntity) {
        // Speichert die übergebene Subscription im Repository
        subscriptionRepository.save(subscriptionEntity);
    }

    /**
     * Löscht eine Subscription anhand ihrer ID.
     *
     * @param id die ID der Subscription, die gelöscht werden soll
     */
    public void deleteById(Long id) {
        // Löscht die Subscription mit der angegebenen ID aus dem Repository
        subscriptionRepository.deleteById(id);
    }
}
