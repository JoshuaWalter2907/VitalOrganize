package com.springboot.vitalorganize.controller;

import com.springboot.vitalorganize.model.RoleAssignment;
import com.springboot.vitalorganize.service.PlannerService;
import com.springboot.vitalorganize.service.UserService;
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
    private PlannerService plannerService; // Service zur Planung von Aufgaben

    @Autowired
    UserService userService; // Service zur Benutzerverwaltung

    private final String WG_ADMIN = "admin"; // Konstante für den Admin-Rollenbezeichner

    /**
     * Zeigt den Planer mit allen Zuweisungen an.
     */
    @GetMapping("/planner")
    public String getPlanner(Model model) {
        List<RoleAssignment> assignments = plannerService.findAll(); // Alle Zuweisungen holen
        boolean isAdmin = false; // Standardwert für Admin

        // Überprüfung, ob der Benutzer ein Admin ist
        if (userService.getCurrentUser().getWg_role() != null &&
                userService.getCurrentUser().getWg_role().equalsIgnoreCase(WG_ADMIN)) {
            isAdmin = true;
        }

        // Zuweisungen und Admin-Status an die View übergeben
        model.addAttribute("assignments", assignments);
        model.addAttribute("isAdmin", isAdmin);
        return "planner"; // Rückgabe der View
    }

    /**
     * Fügt eine neue Zuweisung hinzu.
     */
    @PostMapping("/planner/add")
    public String addAssignment(@RequestParam("week") String weekString,
                                @RequestParam("personName") String personName,
                                @RequestParam("role") String role) {
        LocalDate selectedDate = LocalDate.parse(weekString); // Datum parsen

        // Berechnung der Woche basierend auf dem Datum
        PlannerService.Week week = plannerService.calculateWeek(selectedDate);

        // Neue Zuweisung erstellen und speichern
        RoleAssignment assignment = new RoleAssignment();
        assignment.setRole(role);
        assignment.setPersonName(personName);
        assignment.setWeek(String.valueOf(week));
        plannerService.save(assignment);

        return "redirect:/planner"; // Weiterleitung zur Planer-Seite
    }
}
