package com.springboot.vitalorganize.repository;

import com.springboot.vitalorganize.entity.SubscriptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionRepository extends JpaRepository<SubscriptionEntity, Long> {

}
