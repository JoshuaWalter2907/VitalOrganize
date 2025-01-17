package com.springboot.vitalorganize.dao;

import com.springboot.vitalorganize.model.RoleAssignment;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class RoleAssignmentDAO {

    private final JdbcTemplate jdbcTemplate;

    public RoleAssignmentDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<RoleAssignment> findAll() {
        String sql = "SELECT week, person_name AS personName, role FROM role_assignments";
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(RoleAssignment.class));
    }

    public void save(RoleAssignment assignment) {
        String sql = "INSERT INTO role_assignments (week, person_name, role) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, assignment.getWeek(), assignment.getPersonName(), assignment.getRole());
    }
}
