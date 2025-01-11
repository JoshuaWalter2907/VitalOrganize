package com.springboot.vitalorganize.controller;

import com.springboot.vitalorganize.repository.FundRepository;
import com.springboot.vitalorganize.repository.PaymentRepository;
import com.springboot.vitalorganize.service.GraphService;
import com.springboot.vitalorganize.service.StatisticsService;
import com.springboot.vitalorganize.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@AllArgsConstructor
@RequestMapping("/statistics")
public class StatisticsController {

    private final GraphService graphService;
    private final StatisticsService statisticsService;
    private final PaymentRepository paymentRepository;
    private final UserService userService;
    private final FundRepository fundRepository;

    @GetMapping
    public String showStatistics(@AuthenticationPrincipal OAuth2User user,
                                 OAuth2AuthenticationToken token,
                                 Model model) {
        Long userId  = userService.getCurrentUser(user, token).getId();

        // premium function, requires membership
        if(!userService.getCurrentUser(user, token).isMember()){
            return "home";
        }
        List<Long> fundIds = paymentRepository.findFundsByUser(userId);

        LocalDateTime startDate = LocalDateTime.now().minusDays(30);

        List<Map<String, Object>> fundsStatistics = new ArrayList<>();

        for (Long fundId : fundIds) {
            Map<String, Object> statistics = statisticsService.fetchStatisticsForFund(fundId, startDate);
            List<String> charts = graphService.generateChartsForFund(fundId, startDate);

            String fundName = fundRepository.findNameById(fundId);

            Map<String, Object> fundData = new HashMap<>();
            fundData.put("fundName", fundName);
            fundData.put("fundId", fundId);
            fundData.put("statistics", statistics);
            fundData.put("charts", charts);

            fundsStatistics.add(fundData);
        }

        model.addAttribute("fundsStatistics", fundsStatistics);
        return "statistics/statistics";
    }
}