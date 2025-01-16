package com.springboot.vitalorganize.controller;

import com.springboot.vitalorganize.entity.Profile_User.UserEntity;
import com.springboot.vitalorganize.model.Fund_Statistics.FundStatisticsData;
import com.springboot.vitalorganize.service.StatisticsService;
import com.springboot.vitalorganize.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@AllArgsConstructor
@RequestMapping("/statistics")
public class StatisticsController {

    private final StatisticsService statisticsService;
    private final UserService userService;

    // fetch all statistics for the current user
    @GetMapping
    public String showStatistics(Model model) {
        UserEntity userEntity = userService.getCurrentUser();

        // premium function, requires membership
        if(!userEntity.isMember()){
            return "home";
        }

        List<FundStatisticsData> fundsStatistics = statisticsService.fetchAllFundStatistics();

        model.addAttribute("fundsStatistics", fundsStatistics);
        return "statistics/statistics";
    }
}