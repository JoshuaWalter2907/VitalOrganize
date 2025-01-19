package com.springboot.vitalorganize.dao;

import com.springboot.vitalorganize.model.Meal;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Diese Klasse dient als Datenzugriffsschicht für die "Meal"-Daten.
 * Sie verwendet JdbcTemplate, um auf die Datenbank zuzugreifen und Abfragen auszuführen.
 */
@Repository
public class MealDAO {

    // JdbcTemplate wird verwendet, um SQL-Abfragen gegen die Datenbank auszuführen.
    private final JdbcTemplate jdbcTemplate;

    // Konstruktor zum Initialisieren des JdbcTemplate-Objekts.
    public MealDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Ein RowMapper wird verwendet, um die Ergebnisse einer SQL-Abfrage
     * in ein Meal-Objekt umzuwandeln.
     */
    private final RowMapper<Meal> mealRowMapper = new RowMapper<Meal>() {
        @Override
        public Meal mapRow(ResultSet rs, int rowNum) throws SQLException {
            Meal meal = new Meal();
            meal.setId(rs.getLong("id")); // Setzt die ID des Essens.
            meal.setUserId(rs.getLong("userId")); // Setzt die Benutzer-ID.
            meal.setRecipeId(rs.getLong("recipeId")); // Setzt die Rezept-ID.
            meal.setCookDate(rs.getDate("cookDate").toLocalDate()); // Setzt das Datum, an dem das Essen gekocht wurde.
            return meal;
        }
    };

    /**
     * Fügt ein neues Meal (Essen) in die Datenbank ein.
     * @param meal Das Meal-Objekt, das eingefügt werden soll.
     */
    public void insertMeal(Meal meal) {
        String sql = "INSERT INTO user_meals (userId, recipeId, cookDate) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, meal.getUserId(), meal.getRecipeId(), meal.getCookDate());
    }

    /**
     * Sucht alle Mahlzeiten eines Benutzers, die innerhalb einer bestimmten Woche liegen.
     * @param userId Die ID des Benutzers.
     * @param startDate Das Startdatum der Woche.
     * @param endDate Das Enddatum der Woche.
     * @return Eine Liste von Meal-Objekten.
     */
    public List<Meal> findMealsByUserIdAndWeek(int userId, LocalDate startDate, LocalDate endDate) {
        String sql = "SELECT * FROM user_meals WHERE userId = ? AND cookDate BETWEEN ? AND ?";
        return jdbcTemplate.query(sql, mealRowMapper, userId, startDate, endDate);
    }

    /**
     * Löscht eine Mahlzeit aus der Datenbank anhand ihrer ID.
     * @param id Die ID der zu löschenden Mahlzeit.
     */
    public void deleteMealById(int id) {
        String sql = "DELETE FROM user_meals WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    /**
     * Ruft alle Mahlzeiten eines bestimmten Benutzers ab.
     * @param userId Die ID des Benutzers.
     * @return Eine Liste von Meal-Objekten.
     */
    public List<Meal> findAllMealsForUser(int userId) {
        String sql = "SELECT * FROM user_meals WHERE userId = ?";
        return jdbcTemplate.query(sql, mealRowMapper, userId);
    }

    /**
     * Erstellt einen Bericht über die Mahlzeiten eines Benutzers in einem bestimmten Zeitraum.
     * @param userId Die ID des Benutzers.
     * @param startDate Das Startdatum des Zeitraums.
     * @param endDate Das Enddatum des Zeitraums.
     * @return Eine Liste von Maps, die Informationen über die Mahlzeiten enthalten.
     */
    public List<Map<String, Object>> findMealReportForUser(Long userId, LocalDate startDate, LocalDate endDate) {
        String sql = "SELECT meal_date, recipe_name, calories " +
                "FROM MealReportView " +
                "WHERE userId = ? AND meal_date BETWEEN ? AND ?";
        return jdbcTemplate.queryForList(sql, userId, startDate, endDate);
    }
}
