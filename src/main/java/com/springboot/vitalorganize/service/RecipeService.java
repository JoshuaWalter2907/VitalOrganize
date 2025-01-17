package com.springboot.vitalorganize.service;

import com.springboot.vitalorganize.dao.RecipeDAO;
import com.springboot.vitalorganize.model.Recipe;
import org.springframework.stereotype.Service;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

@Service
public class RecipeService {

    private final RecipeDAO recipeDAO;

    public RecipeService(RecipeDAO recipeDAO) {
        this.recipeDAO = recipeDAO;
    }

    /**
     * Speichert ein neues Rezept in der Datenbank.
     */
    public int createEmptyRecipe(String title) {
        Recipe recipe = new Recipe();
        recipe.setTitle(title);
        // Setze Standardwerte für das neue Rezept (kann später angepasst werden)
        recipe.setDifficulty("Unknown");
        recipe.setPortions(1);
        recipe.setKeywords("");
        recipe.setSource("");
        recipe.setSource_url("");
        recipe.setTotalTime(0.0);
        recipe.setDiet(List.of());
        recipe.setImage_urls(List.of());
        recipe.setIngredients(List.of());
        Recipe.Nutrition nutrition = new Recipe.Nutrition();
        nutrition.setKcal(0);
        recipe.setNutrition(nutrition);
        Recipe.Rating rating = new Recipe.Rating();
        rating.setRatingValue(0.0);
        rating.setRatingCount(0);
        recipe.setRating(rating);
        // Speichern in der Datenbank und gibt die ID des neu erstellten Rezeptes zurück
        return recipeDAO.saveRecipe(recipe);
    }

    /**
     * Lädt alle verfügbaren Rezepte aus der Datenbank.
     */
    public List<Recipe> getAllRecipes() {
        return recipeDAO.loadAllRecipes();
    }

    /**
     * Sucht Rezepte anhand eines Titels.
     */
    public List<Recipe> searchRecipesByTitle(String title) {
        return recipeDAO.loadRecipe(title);
    }

    /**
     * Lädt ein einzelnes Rezept anhand der ID.
     */
    public Recipe getRecipeById(int id) {
        return recipeDAO.loadRecipe(id);
    }


    public void deleteById(Long id) {
        recipeDAO.delete(id);
    }

    public void updateRecipe(Recipe recipe) {
        recipeDAO.update(recipe);
    }

    public void saveRecipe(String title, String difficulty, int portions, String ingredients,
                           Integer calories, Double rating, String source, Double time) {

        Recipe recipe = new Recipe();
        recipe.setTitle(title);
        recipe.setDifficulty(difficulty);
        recipe.setPortions(portions);

        // Zutaten parsen und setzen
        List<Recipe.Ingredient> parsedIngredients = parseIngredients(ingredients);
        recipe.setIngredients(parsedIngredients);

        recipe.setNutrition(new Recipe.Nutrition(calories));
        recipe.setRating(new Recipe.Rating(rating));
        recipe.setSource(source);
        recipe.setTotalTime(time);

        recipeDAO.saveRecipe(recipe); // Methode für das Speichern des Rezepts
    }

    public byte[] generateRecipePdf(Recipe recipe) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Document document = new Document();

        try {
            PdfWriter.getInstance(document, byteArrayOutputStream);
            document.open();

            // Titel
            Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD);
            Paragraph title = new Paragraph("Rezept: " + recipe.getTitle(), titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            // Schwierigkeit
            Font sectionFont = new Font(Font.HELVETICA, 14, Font.BOLD);
            Paragraph difficulty = new Paragraph("Schwierigkeit: " + recipe.getDifficulty(), sectionFont);
            difficulty.setSpacingBefore(10f);
            document.add(difficulty);

            // Zutaten-Tabelle
            PdfPTable table = new PdfPTable(3);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10f);
            table.addCell("Zutat");
            table.addCell("Menge");
            table.addCell("Einheit");
            for (Recipe.Ingredient ingredient : recipe.getIngredients()) {
                table.addCell(ingredient.getName());
                table.addCell(ingredient.getAmount());
                table.addCell(ingredient.getUnit());
            }
            document.add(new Paragraph("Zutaten:"));
            document.add(table);

            // Quelle
            if (recipe.getSource() != null && !recipe.getSource().isEmpty()) {
                Paragraph source = new Paragraph("Quelle: " + recipe.getSource(), sectionFont);
                source.setSpacingBefore(10f);
                document.add(source);
            }

            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Fehler bei der PDF-Generierung", e);
        }

        return byteArrayOutputStream.toByteArray();
    }

    private List<Recipe.Ingredient> parseIngredients(String ingredients) {
        // Entferne die äußeren Klammern und splitte die Zutaten
        ingredients = ingredients.substring(1, ingredients.length() - 1);
        String[] ingredientsArray = ingredients.split(",");

        List<Recipe.Ingredient> parsedIngredients = new ArrayList<>();
        for (String ingredient : ingredientsArray) {
            ingredient = ingredient.trim(); // Entferne Leerzeichen
            String amount = "";
            String unit = "";
            String name = "";

            // RegEx für Zahlen + Einheit
            String[] parts = ingredient.split("\\s+", 2);
            if (parts.length > 1 && parts[0].matches("\\d+[.,]?\\d*|\\d+/\\d+")) {
                amount = parts[0];
                String[] unitAndName = parts[1].split("\\s+", 2);
                if (unitAndName.length > 1) {
                    unit = unitAndName[0];
                    name = unitAndName[1];
                } else {
                    name = unitAndName[0];
                }
            } else {
                name = ingredient; // Kein Mengen- oder Einheitsteil vorhanden
            }

            parsedIngredients.add(new Recipe.Ingredient(amount, name, unit));
        }

        return parsedIngredients;
    }
}
