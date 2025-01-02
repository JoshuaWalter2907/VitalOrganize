package com.springboot.vitalorganize.repository;


import com.springboot.vitalorganize.model.UserEntity;
import com.springboot.vitalorganize.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository  extends JpaRepository<Payment, Long> {
    @Query("SELECT p FROM Payment p ORDER BY p.date DESC LIMIT 1")
    Payment findLatestTransaction();


    List<Payment> findAllByUser(UserEntity profileData);

    @Modifying
    @Query("UPDATE Payment o SET o.user = NULL WHERE o.user.id = :userId")
    void updateUserReferencesToNull(@Param("userId") Long userId);

    @Query("SELECT p FROM Payment p ORDER BY p.date DESC LIMIT 1")
    Payment findLatestTransactionByFundId(Long id);

    void deleteByFundId(Long id);
}
