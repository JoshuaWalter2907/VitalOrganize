package com.springboot.vitalorganize.controller;

import com.springboot.vitalorganize.entity.Profile_User.UserEntity;
import com.springboot.vitalorganize.model.FundStatisticsData;
import com.springboot.vitalorganize.service.StatisticsService;
import com.springboot.vitalorganize.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

/**
 * Controller für die Statistics-Page
 */
@Controller
@AllArgsConstructor
@RequestMapping("/statistics")
public class StatisticsController {

    private final StatisticsService statisticsService;
    private final UserService userService;

    /**
     * Zeigt alle Statistiken des aktuellen Nutzers an
     * @param model Das Model für die View
     * @return Statistics-Page
     */
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