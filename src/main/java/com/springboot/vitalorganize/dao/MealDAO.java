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

@Repository
public class MealDAO {

    private final JdbcTemplate jdbcTemplate;

    public MealDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Meal> mealRowMapper = new RowMapper<Meal>() {
        @Override
        public Meal mapRow(ResultSet rs, int rowNum) throws SQLException {
            Meal meal = new Meal();
            meal.setId(rs.getInt("id"));
            meal.setUserId(rs.getInt("userId"));
            meal.setRecipeId(rs.getInt("recipeId"));
            meal.setCookDate(rs.getDate("cookDate").toLocalDate());
            return meal;
        }
    };

    public void saveMeal(Meal meal) {
        String sql = "INSERT INTO user_meals (userId, recipeId, cookDate) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, meal.getUserId(), meal.getRecipeId(), meal.getCookDate());
    }

    public List<Meal> findMealsByUserIdAndWeek(int userId, LocalDate startDate, LocalDate endDate) {
        String sql = "SELECT * FROM user_meals WHERE userId = ? AND cookDate BETWEEN ? AND ?";
        return jdbcTemplate.query(sql, mealRowMapper, userId, startDate, endDate);
    }

    public void deleteMealById(int id) {
        String sql = "DELETE FROM user_meals WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    public List<Meal> findAllMealsForUser(int userId) {
        String sql = "SELECT * FROM user_meals WHERE userId = ?";
        return jdbcTemplate.query(sql, mealRowMapper, userId);
    }

    public List<Map<String, Object>> findMealReportForUser(int userId, LocalDate startDate, LocalDate endDate) {
        String sql = "SELECT meal_date, recipe_name, calories " +
                "FROM MealReportView " +
                "WHERE userId = ? AND meal_date BETWEEN ? AND ?";
        return jdbcTemplate.queryForList(sql, userId, startDate, endDate);
    }
}
