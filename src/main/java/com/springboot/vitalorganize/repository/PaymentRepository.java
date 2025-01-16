package com.springboot.vitalorganize.repository;


import com.springboot.vitalorganize.entity.Fund_Payments.PaymentEntity;
import com.springboot.vitalorganize.entity.Profile_User.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository  extends JpaRepository<PaymentEntity, Long> {

    PaymentEntity findFirstByOrderByDateDesc();

    PaymentEntity findFirstByFundIdOrderByDateDesc(Long fundId);

    List<PaymentEntity> findAllByUser(UserEntity profileData);

    List<PaymentEntity> findByUserId(Long userId);


    // get the balance on the startDate
    @Query("SELECT p.balance " +
            "FROM PaymentEntity p " +
            "WHERE p.fund.id = :fundId " +
            "AND p.date <= :startDate " +
            "ORDER BY p.date ASC LIMIT 1")
    Optional<Double> findBalanceByDate(@Param("fundId") Long fundId,
                                       @Param("startDate") LocalDateTime startDate);

    // get the net daily transactions for the last 30 days
    @Query("SELECT p.fund.id, p.date, " +
            "SUM(CASE WHEN p.type = 'EINZAHLEN' THEN p.amount ELSE (0 - p.amount) END) AS netAmount, " +
            "SUM(CASE WHEN p.type = 'EINZAHLEN' THEN p.amount ELSE 0 END) AS totalDeposits, " +
            "SUM(CASE WHEN p.type = 'AUSZAHLEN' THEN p.amount ELSE 0 END) AS totalWithdrawals " +
            "FROM PaymentEntity p WHERE p.fund.id = :fundId AND p.date >= :startDate " +
            "GROUP BY p.date ORDER BY p.date")
    List<Object[]> findDailyTransactionsByFund(@Param("fundId") Long fundId,
                                               @Param("startDate") LocalDateTime startDate);
}
