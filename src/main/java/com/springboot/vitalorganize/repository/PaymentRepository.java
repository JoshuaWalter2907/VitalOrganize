package com.springboot.vitalorganize.repository;


import com.springboot.vitalorganize.model.Payment;
import com.springboot.vitalorganize.model.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository  extends JpaRepository<Payment, Long> {
    @Query("SELECT p FROM Payment p ORDER BY p.date DESC LIMIT 1")
    Payment findLatestTransaction();


    List<Payment> findAllByUser(UserEntity profileData);

    @Modifying
    @Query("UPDATE Payment o SET o.user = NULL WHERE o.user.id = :userId")
    void updateUserReferencesToNull(@Param("userId") Long userId);

    @Query("SELECT p FROM Payment p WHERE p.fund.id = :id ORDER BY p.date DESC LIMIT 1")
    Payment findLatestTransactionByFundId(@Param("id") Long id);


    void deleteByFundId(Long id);


    @Query("SELECT p.fund.id, p.date, " +
            "SUM(CASE WHEN p.type = 'EINZAHLEN' THEN p.amount ELSE (0 - p.amount) END) AS netAmount, " +
            "SUM(CASE WHEN p.type = 'EINZAHLEN' THEN p.amount ELSE 0 END) AS totalDeposits, " +
            "SUM(CASE WHEN p.type = 'AUSZAHLEN' THEN p.amount ELSE 0 END) AS totalWithdrawals " +
            "FROM Payment p WHERE p.fund.id = :fundId AND p.date >= :startDate " +
            "GROUP BY p.date ORDER BY p.date")
    List<Object[]> findDailyTransactionsByFund(@Param("fundId") Long fundId,
                                               @Param("startDate") LocalDateTime startDate);

    @Query("SELECT DISTINCT p.fund.id FROM Payment p WHERE p.user.id = :userId")
    List<Long> findFundsByUser(@Param("userId") Long userId);

    @Query("SELECT p.balance " +
            "FROM Payment p " +
            "WHERE p.fund.id = :fundId " +
            "AND p.date <= :startDate " +
            "ORDER BY p.date ASC LIMIT 1")
    Optional<Double> findBalanceByDate(@Param("fundId") Long fundId, @Param("startDate") LocalDateTime startDate);}
