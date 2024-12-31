package com.springboot.vitalorganize.service.repositoryhelper;

import com.springboot.vitalorganize.model.FundEntity;
import com.springboot.vitalorganize.repository.FundRepository;
import com.springboot.vitalorganize.repository.PaymentRepository;
import com.springboot.vitalorganize.model.Zahlung;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class FundRepositoryService {

    private final FundRepository fundRepository;
    private final PaymentRepository paymentRepository;

    public FundRepositoryService(FundRepository fundRepository, PaymentRepository paymentRepository) {
        this.fundRepository = fundRepository;
        this.paymentRepository = paymentRepository;
    }

    public Optional<FundEntity> findFundById(Long fundId) {
        return fundRepository.findById(fundId);
    }

    public List<FundEntity> findFundsByUserId(Long userId) {
        return fundRepository.findFundsByUserId(userId);
    }

    public void saveFund(FundEntity fund) {
        fundRepository.save(fund);
    }

    public void deleteFund(FundEntity fund) {
        paymentRepository.deleteByFundId(fund.getId()); // Zahlungen löschen
        fundRepository.delete(fund); // Fund löschen
    }

    public void deletePaymentsByFundId(Long fundId) {
        paymentRepository.deleteByFundId(fundId);
    }

    public List<Zahlung> findPaymentsByFundId(Long fundId) {
        FundEntity fund = fundRepository.findById(fundId).orElseThrow(() -> new IllegalArgumentException("Fund mit ID " + fundId + " nicht gefunden"));
        return fund.getPayments();
    }

    public double getLatestBalanceForFund(FundEntity fund) {
        if (fund.getPayments().isEmpty()) {
            return 0.0;
        }
        return fund.getPayments().getLast().getBalance();
    }



}
