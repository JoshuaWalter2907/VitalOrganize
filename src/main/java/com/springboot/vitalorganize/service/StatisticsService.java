package com.springboot.vitalorganize.service;

import com.springboot.vitalorganize.model.FundStatisticsData;
import com.springboot.vitalorganize.repository.FundRepository;
import com.springboot.vitalorganize.repository.PaymentRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@AllArgsConstructor
public class StatisticsService {

    private final PaymentRepository paymentRepository;
    private final UserService userService;
    private final GraphService graphService;
    private final FundRepository fundRepository;

    // fetch all statistics for all funds for the current user
    public List<FundStatisticsData> fetchAllFundStatistics(){
        Long userId = userService.getCurrentUser().getId();

        List<Long> fundIds = fundRepository.findFundIdsByUserId(userId);
        LocalDateTime startDate = LocalDateTime.now().minusDays(30);
        List<FundStatisticsData> fundsStatistics = new ArrayList<>();

        for (Long fundId : fundIds) {
            Map<String, Object> statistics = fetchStatisticsForFund(fundId, startDate);
            List<String> charts = graphService.generateFundCharts(fundId, startDate);

            String fundName = fundRepository.findNameById(fundId);

            FundStatisticsData fundStatisticsData = new FundStatisticsData(fundName, fundId, statistics, charts);
            fundsStatistics.add(fundStatisticsData);
        }
        return fundsStatistics;
    }

    // fetch statistics for a fund
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
