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

    /**
     * Gibt alle RoleAssignments zurück.
     *
     * @return Eine Liste aller RoleAssignments aus der Datenbank.
     */
    public List<RoleAssignment> findAll(){
        return roleAssignmentDAO.findAll();  // Ruft alle RoleAssignments aus der Datenbank ab
    }

    /**
     * Speichert ein RoleAssignment in der Datenbank.
     *
     * @param roleAssignment Das zu speichernde RoleAssignment.
     */
    public void save(RoleAssignment roleAssignment) {
        roleAssignmentDAO.save(roleAssignment);  // Speichert das RoleAssignment in der Datenbank
    }

    /**
     * Berechnet den Montag und Sonntag der Woche basierend auf dem angegebenen Datum.
     *
     * @param selectedDate Das Datum, anhand dessen die Woche berechnet werden soll.
     * @return Ein Week-Objekt, das den Wochenbereich von Montag bis Sonntag enthält.
     */
    public Week calculateWeek(LocalDate selectedDate) {
        // Bestimme den Montag der Woche
        LocalDate startOfWeek = selectedDate.with(DayOfWeek.MONDAY);

        // Bestimme den Sonntag der Woche
        LocalDate endOfWeek = selectedDate.with(DayOfWeek.SUNDAY);

        // Gebe ein Week-Objekt zurück, das den Wochenbereich enthält
        return new Week(startOfWeek, endOfWeek);
    }

    // Wochenbereich
    public static class Week {
        private LocalDate startOfWeek;
        private LocalDate endOfWeek;

        // Konstruktor, der den Wochenbereich setzt
        public Week(LocalDate startOfWeek, LocalDate endOfWeek) {
            this.startOfWeek = startOfWeek;
            this.endOfWeek = endOfWeek;
        }

        // Gibt das Startdatum der Woche zurück
        public LocalDate getStartOfWeek() {
            return startOfWeek;
        }

        // Gibt das Enddatum der Woche zurück
        public LocalDate getEndOfWeek() {
            return endOfWeek;
        }

        // Gibt eine String-Darstellung des Wochenbereichs zurück
        @Override
        public String toString() {
            return startOfWeek + " - " + endOfWeek;
        }
    }
}
