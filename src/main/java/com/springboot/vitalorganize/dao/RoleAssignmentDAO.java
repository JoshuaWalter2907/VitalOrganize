package com.springboot.vitalorganize.dao;

import com.springboot.vitalorganize.model.RoleAssignment;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Diese Klasse dient als Datenzugriffsschicht für Rollen-Zuweisungen.
 * Sie ermöglicht das Abrufen und Speichern von Daten aus/in der Datenbank.
 */
@Repository
public class RoleAssignmentDAO {

    // JdbcTemplate wird verwendet, um SQL-Abfragen gegen die Datenbank auszuführen.
    private final JdbcTemplate jdbcTemplate;

    // Konstruktor zum Initialisieren des JdbcTemplate-Objekts.
    public RoleAssignmentDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Ruft alle Rollen-Zuweisungen aus der Datenbank ab.
     * @return Eine Liste von RoleAssignment-Objekten, die alle Zuweisungen enthalten.
     */
    public List<RoleAssignment> findAll() {
        // SQL-Abfrage, um alle Zuweisungen abzurufen.
        String sql = "SELECT week, person_name AS personName, role FROM role_assignments";

        // Die Ergebnisse der Abfrage werden automatisch in RoleAssignment-Objekte umgewandelt.
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(RoleAssignment.class));
    }

    /**
     * Speichert eine neue Rollen-Zuweisung in der Datenbank.
     * @param assignment Die Rollen-Zuweisung, die gespeichert werden soll.
     */
    public void save(RoleAssignment assignment) {
        // SQL-Abfrage, um eine neue Zuweisung einzufügen.
        String sql = "INSERT INTO role_assignments (week, person_name, role) VALUES (?, ?, ?)";

        // Führt die Abfrage aus und übergibt die Werte aus dem Assignment-Objekt.
        jdbcTemplate.update(sql, assignment.getWeek(), assignment.getPersonName(), assignment.getRole());
    }
}