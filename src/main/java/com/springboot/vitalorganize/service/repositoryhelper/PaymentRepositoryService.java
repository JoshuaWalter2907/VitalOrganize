package com.springboot.vitalorganize.service.repositoryhelper;

import com.springboot.vitalorganize.model.UserEntity;
import com.springboot.vitalorganize.model.Payment;
import com.springboot.vitalorganize.repository.PaymentRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class PaymentRepositoryService {


    private final PaymentRepository paymentRepository;

    public List<Payment> findPaymentsByUser(UserEntity user) {
        return paymentRepository.findAllByUser(user); // Angenommen, PaymentRepository hat diese Methode
    }


    public Optional<Payment> findLatestTransactionByFundId(Long id) {
        return paymentRepository.findLatestTransactionByFundId(id);
    }

    public void savePayment(Payment payment) {
        paymentRepository.save(payment);
    }

    public Payment findLatestTransaction() {
        return paymentRepository.findLatestTransaction();
    }

    public void updateUserReferencesToNull(Long id) {
        paymentRepository.updateUserReferencesToNull(id);
    }
}
