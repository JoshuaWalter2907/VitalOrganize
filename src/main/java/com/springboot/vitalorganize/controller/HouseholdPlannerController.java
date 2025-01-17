package com.springboot.vitalorganize.controller;

import com.springboot.vitalorganize.dao.RoleAssignmentDAO;
import com.springboot.vitalorganize.model.RoleAssignment;
import com.springboot.vitalorganize.service.PlannerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

@Controller
public class HouseholdPlannerController {

    @Autowired
    private PlannerService plannerService;

    @GetMapping("/planner")
    public String getPlanner(Model model) {
        List<RoleAssignment> assignments = plannerService.findAll();
        model.addAttribute("assignments", assignments);
        model.addAttribute("isAdmin", true); // Für Demo. Anpassen mit echter Logik.\n
        return "planner";
    }

    @PostMapping("/planner/add")
    public String addAssignment(@RequestParam("week") String weekString, @RequestParam("personName") String personName, @RequestParam("role") String role) {
        // Das ausgewählte Datum als LocalDate einlesen
        LocalDate selectedDate = LocalDate.parse(weekString);

        // Die Woche (Montag bis Sonntag) berechnen
        PlannerService.Week week = plannerService.calculateWeek(selectedDate);

        //Zuweisung speichern
        RoleAssignment assignment = new RoleAssignment();
        assignment.setRole(role);
        assignment.setPersonName(personName);
        assignment.setWeek(String.valueOf(week));
        plannerService.save(assignment);

        return "redirect:/planner"; // Oder eine andere View
    }
}

