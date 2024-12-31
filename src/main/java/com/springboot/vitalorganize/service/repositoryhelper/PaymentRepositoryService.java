package com.springboot.vitalorganize.service.repositoryhelper;

import com.springboot.vitalorganize.model.UserEntity;
import com.springboot.vitalorganize.model.Zahlung;
import com.springboot.vitalorganize.repository.PaymentRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class PaymentRepositoryService {


    private final PaymentRepository paymentRepository;

    public void deletePaymentsByFundId(Long fundId) {
        paymentRepository.deleteByFundId(fundId);
    }

    public List<Zahlung> findPaymentsByUser(UserEntity user) {
        return paymentRepository.findAllByUser(user); // Angenommen, PaymentRepository hat diese Methode
    }


}
