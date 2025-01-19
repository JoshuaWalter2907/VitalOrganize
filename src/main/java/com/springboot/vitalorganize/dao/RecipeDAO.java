package com.springboot.vitalorganize.dao;

import com.springboot.vitalorganize.model.Recipe;
import com.springboot.vitalorganize.model.Recipe.Ingredient;
import com.springboot.vitalorganize.model.Recipe.Nutrition;
import com.springboot.vitalorganize.model.Recipe.Rating;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class RecipeDAO {

    private final JdbcTemplate jdbcTemplate;

    public RecipeDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Speichert ein Rezept in der Datenbank.
     */
    public int insertRecipe(Recipe recipe) {
        // Speichert das Hauptrezept
        String insertRecipeSql = """
            INSERT INTO Recipe (difficulty, keywords, nutrition_kcal, portions, source, source_url, title, 
                        total_time, user_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        jdbcTemplate.update(insertRecipeSql,
                recipe.getDifficulty(),
                recipe.getKeywords(),
                recipe.getNutrition().getKcal(),
                recipe.getPortions(),
                recipe.getSource(),
                recipe.getSource_url(),
                recipe.getTitle(),
                recipe.getTotalTime(),
                recipe.getUser_id());

        // Holen der generierten ID
        Integer recipeId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Integer.class);

        if (recipeId == null) {
            throw new RuntimeException("Fehler beim Abrufen der generierten Rezept-ID");
        }

        // Speichern der Diät-Daten
        if (recipe.getDiet() != null) {
            String insertDietSql = "INSERT INTO RecipeDiet (recipe_id, diet) VALUES (?, ?)";
            for (String diet : recipe.getDiet()) {
                jdbcTemplate.update(insertDietSql, recipeId, diet);
            }
        }

        // Speichern der Zutaten
        String insertIngredientSql = "INSERT INTO RecipeIngredient (recipe_id, name, amount, unit) VALUES (?, ?, ?, ?)";
        for (Ingredient ingredient : recipe.getIngredients()) {
            jdbcTemplate.update(insertIngredientSql,
                    recipeId,
                    ingredient.getName(),
                    ingredient.getAmount(),
                    ingredient.getUnit());
        }

        // Speichern der Bilder
        if (recipe.getImage_urls() != null) {
            String insertImageSql = "INSERT INTO RecipeImage (recipe_id, image_url) VALUES (?, ?)";
            for (String imageUrl : recipe.getImage_urls()) {
                jdbcTemplate.update(insertImageSql, recipeId, imageUrl);
            }
        }

        // Speichern der Bewertung
        String insertRatingSql = "INSERT INTO RecipeRating (recipe_id, rating_value, rating_count) VALUES (?, ?, ?)";
        Rating rating = recipe.getRating();
        jdbcTemplate.update(insertRatingSql, recipeId, rating.getRatingValue(), rating.getRatingCount());

        return recipeId;
    }

    /**
     * Lädt ein Rezept aus der Datenbank anhand der ID.
     */
    public Recipe loadRecipe(int recipeId) {
        String sql = "SELECT * FROM RecipeView WHERE recipe_id = ?";
        return jdbcTemplate.queryForObject(sql, new RecipeViewRowMapper(), recipeId);
    }

    /**
     * Lädt eine Liste an Rezepten aus der Datenbank anhand eines Schlagwortens.
     */
    public List<Recipe> loadRecipe(String name) {
        String sql = "SELECT * FROM RecipeView WHERE title LIKE ?";
        return jdbcTemplate.query(sql, new RecipeViewRowMapper(), "%" + name + "%");
    }
    public List<Recipe> loadRecipeForUser(String name, Long id) {
        String sql = "SELECT * FROM RecipeView WHERE title LIKE ? AND user_id = ?";
        return jdbcTemplate.query(sql, new RecipeViewRowMapper(), "%" + name + "%", id);
    }
    /**
     * Lädt eine Liste mit allen Rezepten
     */
    public List<Recipe> loadAllRecipes() {
        String sql = "SELECT * FROM RecipeView";
        return jdbcTemplate.query(sql, new RecipeViewRowMapper());
    }
    public List<Recipe> loadAllRecipesForUser(Long id) {
        String sql = "SELECT * FROM RecipeView WHERE user_id = ?";
        return jdbcTemplate.query(sql, new RecipeViewRowMapper(), id);
    }

    public void delete(Long id) {
        String sql = "DELETE FROM Recipe WHERE id = " + id;
        jdbcTemplate.execute(sql);
        System.out.println();
    }

    public void update(Recipe recipe) {
        // Hauptrezept-Daten aktualisieren
        String updateRecipeSql = """
        UPDATE Recipe
        SET difficulty = ?, keywords = ?, nutrition_kcal = ?, portions = ?, source = ?, source_url = ?, title = ?, total_time = ?
        WHERE id = ?
    """;

        jdbcTemplate.update(updateRecipeSql,
                recipe.getDifficulty(),
                recipe.getKeywords(),
                recipe.getNutrition().getKcal(),
                recipe.getPortions(),
                recipe.getSource(),
                recipe.getSource_url(),
                recipe.getTitle(),
                recipe.getTotalTime(),
                recipe.getId());

        // Diät-Daten aktualisieren (bestehende Einträge löschen und neu einfügen)
        String deleteDietSql = "DELETE FROM RecipeDiet WHERE recipe_id = ?";
        jdbcTemplate.update(deleteDietSql, recipe.getId());

        String insertDietSql = "INSERT INTO RecipeDiet (recipe_id, diet) VALUES (?, ?)";
        for (String diet : recipe.getDiet()) {
            jdbcTemplate.update(insertDietSql, recipe.getId(), diet);
        }

        // Zutaten aktualisieren (bestehende Einträge löschen und neu einfügen)
        String deleteIngredientSql = "DELETE FROM RecipeIngredient WHERE recipe_id = ?";
        jdbcTemplate.update(deleteIngredientSql, recipe.getId());

        String insertIngredientSql = "INSERT INTO RecipeIngredient (recipe_id, name, amount, unit) VALUES (?, ?, ?, ?)";
        for (Ingredient ingredient : recipe.getIngredients()) {
            jdbcTemplate.update(insertIngredientSql,
                    recipe.getId(),
                    ingredient.getName(),
                    ingredient.getAmount(),
                    ingredient.getUnit());
        }

    }

    private static class RecipeViewRowMapper implements RowMapper<Recipe> {

        @Override
        public Recipe mapRow(ResultSet rs, int rowNum) throws SQLException {
            Recipe recipe = new Recipe();
            recipe.setId(rs.getString("recipe_id"));
            recipe.setTitle(rs.getString("title"));
            recipe.setDifficulty(rs.getString("difficulty"));
            recipe.setKeywords(rs.getString("keywords"));
            recipe.setPortions(rs.getInt("portions"));
            recipe.setSource(rs.getString("source"));
            recipe.setSource_url(rs.getString("source_url"));
            recipe.setTotalTime(rs.getDouble("total_time"));

            // Nutrition
            Nutrition nutrition = new Nutrition();
            nutrition.setKcal(rs.getInt("nutrition_kcal"));
            recipe.setNutrition(nutrition);

            // Diets
            String diets = rs.getString("diets");
            recipe.setDiet(diets != null ? List.of(diets.split(",")) : List.of());

            // Images
            String images = rs.getString("image_urls");
            recipe.setImage_urls(images != null ? List.of(images.split(",")) : List.of());

            // Ingredients
            String ingredients = rs.getString("ingredients");
            if (ingredients != null) {
                List<Recipe.Ingredient> ingredientList = List.of(ingredients.split(",")).stream().map(ingredient -> {
                    String[] parts = ingredient.split("\\|");
                    Recipe.Ingredient ing = new Recipe.Ingredient();
                    ing.setName(parts[0]);
                    ing.setAmount(parts.length > 1 ? parts[1] : "");
                    ing.setUnit(parts.length > 2 ? parts[2] : "");
                    return ing;
                }).toList();
                recipe.setIngredients(ingredientList);
            }

            // Rating
            Rating rating = new Rating();
            rating.setRatingValue(rs.getDouble("rating_value"));
            rating.setRatingCount(rs.getInt("rating_count"));
            recipe.setRating(rating);

            return recipe;
        }
    }
}