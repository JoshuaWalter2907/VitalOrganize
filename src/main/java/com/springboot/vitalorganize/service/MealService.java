package com.springboot.vitalorganize.service;


import com.springboot.vitalorganize.dao.MealDAO;
import com.springboot.vitalorganize.model.Meal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

@Service
public class MealService {

    private final MealDAO mealDAO;

    public MealService(MealDAO mealDAO) {
        this.mealDAO = mealDAO;
    }

    public List<Meal> getMealsForUserInWeek(int userId, LocalDate anyDateInWeek) throws SQLException {
        LocalDate startOfWeek = anyDateInWeek.with(java.time.DayOfWeek.MONDAY);
        LocalDate endOfWeek = anyDateInWeek.with(java.time.DayOfWeek.SUNDAY);
        return mealDAO.findMealsByUserIdAndWeek(userId, startOfWeek, endOfWeek);
    }

    public void addMeal(int userId, int recipeId, LocalDate cookDate) throws SQLException {
        Meal meal = new Meal();
        meal.setUserId(userId);
        meal.setRecipeId(recipeId);
        meal.setCookDate(cookDate);
        mealDAO.saveMeal(meal);
    }

    public void deleteMeal(int mealId) throws SQLException {
        mealDAO.deleteMealById(mealId);
    }

    public List<Meal> getAllMealsForUser(int userId) throws SQLException {
        return mealDAO.findAllMealsForUser(userId);
    }
}
