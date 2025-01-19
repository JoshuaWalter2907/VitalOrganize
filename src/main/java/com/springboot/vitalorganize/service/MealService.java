package com.springboot.vitalorganize.service;

import com.springboot.vitalorganize.dao.MealDAO;
import com.springboot.vitalorganize.model.Meal;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class MealService {

    private final MealDAO mealDAO;

    public MealService(MealDAO mealDAO) {
        this.mealDAO = mealDAO;
    }

    /**
     * Gibt alle Mahlzeiten eines Benutzers für eine bestimmte Woche zurück.
     *
     * @param userId Die ID des Benutzers, dessen Mahlzeiten abgefragt werden sollen.
     * @param anyDateInWeek Ein beliebiges Datum innerhalb der Woche, um die Woche zu berechnen.
     * @return Eine Liste der Mahlzeiten des Benutzers in der angegebenen Woche.
     */
    public List<Meal> getMealsForUserInWeek(int userId, LocalDate anyDateInWeek) {
        // Berechnet den ersten Tag der Woche (Montag)
        LocalDate startOfWeek = anyDateInWeek.with(java.time.DayOfWeek.MONDAY);
        // Berechnet den letzten Tag der Woche (Sonntag)
        LocalDate endOfWeek = anyDateInWeek.with(java.time.DayOfWeek.SUNDAY);
        // Ruft die Mahlzeiten des Benutzers für den Zeitraum der Woche aus der Datenbank ab
        return mealDAO.findMealsByUserIdAndWeek(userId, startOfWeek, endOfWeek);
    }

    /**
     * Fügt eine neue Mahlzeit für den Benutzer in die Datenbank ein.
     *
     * @param userId Die ID des Benutzers, dem die Mahlzeit zugeordnet werden soll.
     * @param recipeId Die ID des Rezepts, das gekocht wurde.
     * @param cookDate Das Datum, an dem die Mahlzeit gekocht wurde.
     */
    public void addMeal(Long userId, Long recipeId, LocalDate cookDate) {
        Meal meal = new Meal();  // Erstellt ein neues Meal-Objekt
        meal.setUserId(userId);  // Setzt die Benutzer-ID
        meal.setRecipeId(recipeId);  // Setzt die Rezept-ID
        meal.setCookDate(cookDate);  // Setzt das Kochdatum
        mealDAO.insertMeal(meal);  // Fügt die Mahlzeit in die Datenbank ein
    }

    /**
     * Löscht eine Mahlzeit anhand der Mahlzeiten-ID.
     *
     * @param mealId Die ID der Mahlzeit, die gelöscht werden soll.
     */
    public void deleteMeal(int mealId) {
        mealDAO.deleteMealById(mealId);  // Löscht die Mahlzeit aus der Datenbank
    }

    /**
     * Gibt alle Mahlzeiten für einen bestimmten Benutzer zurück.
     *
     * @param userId Die ID des Benutzers, dessen Mahlzeiten abgefragt werden sollen.
     * @return Eine Liste aller Mahlzeiten des Benutzers.
     */
    public List<Meal> getAllMealsForUser(int userId) {
        return mealDAO.findAllMealsForUser(userId);  // Ruft alle Mahlzeiten des Benutzers aus der Datenbank ab
    }
}
