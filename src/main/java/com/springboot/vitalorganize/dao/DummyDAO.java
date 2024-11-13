package com.springboot.vitalorganize.dao;

import com.springboot.vitalorganize.model.Dummy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

@Repository
public class DummyDAO {

    private final JdbcTemplate jdbcTemplate;

    public DummyDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Methode zum Abrufen eines Dummy-Eintrags per ID
    public Dummy findById(int id) {
        String sql = "SELECT * FROM DummyTable WHERE ID = ?";
        return jdbcTemplate.queryForObject(sql, new DummyRowMapper(), id);
    }

    // Methode zum Abrufen aller Dummy-Einträge
    public List<Dummy> findAll() {
        String sql = "SELECT * FROM DummyTable";
        return jdbcTemplate.query(sql, new DummyRowMapper());
    }

    // Methode zum Einfügen eines neuen Dummy-Eintrags
    public int save(Dummy dummy) {
        String sql = "INSERT INTO DummyTable (Name, Date) VALUES (?, ?)";
        return jdbcTemplate.update(sql, dummy.getName(), dummy.getDate());
    }

    // Methode zum Löschen eines Dummy-Eintrags per ID
    public int deleteById(int id) {
        String sql = "DELETE FROM DummyTable WHERE ID = ?";
        return jdbcTemplate.update(sql, id);
    }

    // RowMapper zum Konvertieren der ResultSet-Zeilen in Dummy-Objekte
    private static class DummyRowMapper implements RowMapper<Dummy> {
        @Override
        public Dummy mapRow(ResultSet rs, int rowNum) throws SQLException {
            Dummy dummy = new Dummy();
            dummy.setId(rs.getInt("ID"));
            dummy.setName(rs.getString("Name"));
            dummy.setDate(rs.getDate("Date").toLocalDate());
            return dummy;
        }
    }
}
