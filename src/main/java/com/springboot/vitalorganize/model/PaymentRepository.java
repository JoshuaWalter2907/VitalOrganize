package com.springboot.vitalorganize.model;


import com.paypal.api.payments.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface PaymentRepository  extends JpaRepository<Zahlung, Long> {
    @Query("SELECT p FROM Zahlung p ORDER BY p.date DESC LIMIT 1")
    Optional<Zahlung> findLatestTransaction();


}
