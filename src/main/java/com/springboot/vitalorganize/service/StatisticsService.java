package com.springboot.vitalorganize.service;

import com.springboot.vitalorganize.repository.PaymentRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@AllArgsConstructor
public class StatisticsService {

    private final PaymentRepository paymentRepository;

    public Map<String, Object> fetchStatisticsForFund(Long fundId, LocalDateTime startDate) {
        List<Object[]> dailyData = paymentRepository.findDailyTransactionsByFund(fundId, startDate);

        double totalDeposits = 0;
        double totalWithdrawals = 0;

        for (Object[] record : dailyData) {
            totalDeposits += (double) record[3];
            totalWithdrawals += (double) record[4];
        }

        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalDeposits", totalDeposits);
        statistics.put("totalWithdrawals", totalWithdrawals);

        return statistics;
    }
}
