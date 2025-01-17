package com.springboot.vitalorganize.service;

import com.springboot.vitalorganize.dao.RoleAssignmentDAO;
import com.springboot.vitalorganize.model.RoleAssignment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@Service
public class PlannerService {

    private RoleAssignmentDAO roleAssignmentDAO;

    @Autowired
    PlannerService(RoleAssignmentDAO roleAssignmentDAO) {
        this.roleAssignmentDAO = roleAssignmentDAO;
    }

    public List<RoleAssignment> findAll(){
        return roleAssignmentDAO.findAll();
    }

    public void save(RoleAssignment roleAssignment) {
        roleAssignmentDAO.save(roleAssignment);
    }

    // Berechnet den Montag und Sonntag der Woche basierend auf dem angegebenen Datum
    public Week calculateWeek(LocalDate selectedDate) {
        // Bestimme den Montag der Woche
        LocalDate startOfWeek = selectedDate.with(DayOfWeek.MONDAY);
        
        // Bestimme den Sonntag der Woche
        LocalDate endOfWeek = selectedDate.with(DayOfWeek.SUNDAY);
        
        // Gebe ein Week-Objekt zurück, das den Wochenbereich enthält
        return new Week(startOfWeek, endOfWeek);
    }

    //Wochenbereich
    public static class Week {
        private LocalDate startOfWeek;
        private LocalDate endOfWeek;

        public Week(LocalDate startOfWeek, LocalDate endOfWeek) {
            this.startOfWeek = startOfWeek;
            this.endOfWeek = endOfWeek;
        }

        public LocalDate getStartOfWeek() {
            return startOfWeek;
        }

        public LocalDate getEndOfWeek() {
            return endOfWeek;
        }

        @Override
        public String toString() {
            return "Woche: " + startOfWeek + " bis " + endOfWeek;
        }
    }
}
