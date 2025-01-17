package com.springboot.vitalorganize.repository;

import com.springboot.vitalorganize.entity.Fund_Payments.FundEntity;
import com.springboot.vitalorganize.entity.Fund_Payments.PaymentStatisticsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface ZahlungStatistikRepository extends JpaRepository<PaymentStatisticsEntity, Long> {

    List<PaymentStatisticsEntity> findAllByfund(FundEntity fund);
}
