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
     * Erstellt ein leeres Rezept und speichert es in der Datenbank.
     *
     * @param title Der Titel des neuen Rezepts.
     * @param user_id Die Benutzer-ID des Nutzers, dem das Rezept zugeordnet wird.
     * @return Die ID des neu erstellten Rezepts.
     */
    public int createEmptyRecipe(String title, Long user_id) {
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
        recipe.setUser_id(user_id);
        // Speichern in der Datenbank und gibt die ID des neu erstellten Rezeptes zurück
        return recipeDAO.insertRecipe(recipe);
    }

    /**
     * Lädt alle verfügbaren Rezepte aus der Datenbank.
     *
     * @return Eine Liste aller Rezepte aus der Datenbank.
     */
    public List<Recipe> getAllRecipes() {
        return recipeDAO.loadAllRecipes();  // Ruft alle Rezepte aus der Datenbank ab
    }

    /**
     * Sucht Rezepte anhand eines Titels und einer Benutzer-ID.
     *
     * @param title Der Titel des Rezepts, nach dem gesucht wird.
     * @param id Die Benutzer-ID des Nutzers.
     * @return Eine Liste der gefundenen Rezepte.
     */
    public List<Recipe> searchRecipesByTitleForUser(String title, Long id) {
        return recipeDAO.loadRecipeForUser(title, id);  // Ruft Rezepte basierend auf Titel und Benutzer-ID ab
    }

    /**
     * Lädt ein einzelnes Rezept anhand der Rezept-ID.
     *
     * @param id Die ID des Rezepts, das geladen werden soll.
     * @return Das Rezept mit der angegebenen ID.
     */
    public Recipe getRecipeById(int id) {
        return recipeDAO.loadRecipe(id);  // Ruft ein Rezept basierend auf der ID ab
    }

    /**
     * Löscht ein Rezept anhand der ID.
     *
     * @param id Die ID des zu löschenden Rezepts.
     */
    public void deleteById(Long id) {
        recipeDAO.delete(id);  // Löscht das Rezept aus der Datenbank
    }

    /**
     * Aktualisiert ein bestehendes Rezept.
     *
     * @param recipe Das Rezept, das aktualisiert werden soll.
     */
    public void updateRecipe(Recipe recipe) {
        recipeDAO.update(recipe);  // Aktualisiert das Rezept in der Datenbank
    }

    /**
     * Speichert ein Rezept mit den angegebenen Details in der Datenbank.
     *
     * @param title Der Titel des Rezepts.
     * @param difficulty Die Schwierigkeit des Rezepts.
     * @param portions Die Anzahl der Portionen.
     * @param ingredients Die Zutaten des Rezepts.
     * @param calories Die Kalorienzahl des Rezepts.
     * @param rating Die Bewertung des Rezepts.
     * @param source Die Quelle des Rezepts.
     * @param time Die Zubereitungszeit des Rezepts.
     * @param user_id Die Benutzer-ID des Nutzers, dem das Rezept zugeordnet wird.
     */
    public void saveRecipe(String title, String difficulty, int portions, String ingredients,
                           Integer calories, Double rating, String source, Double time, Long user_id) {

        Recipe recipe = new Recipe();
        recipe.setTitle(title);
        recipe.setDifficulty(difficulty);
        recipe.setPortions(portions);
        recipe.setUser_id(user_id);

        // Zutaten parsen und setzen
        List<Recipe.Ingredient> parsedIngredients = parseIngredients(ingredients);
        recipe.setIngredients(parsedIngredients);

        recipe.setNutrition(new Recipe.Nutrition(calories));  // Setzt die Kalorien
        recipe.setRating(new Recipe.Rating(rating));  // Setzt die Bewertung
        recipe.setSource(source);  // Setzt die Quelle des Rezepts
        recipe.setTotalTime(time);  // Setzt die Zubereitungszeit

        recipeDAO.insertRecipe(recipe);  // Speichert das Rezept in der Datenbank
    }

    /**
     * Generiert ein PDF-Dokument mit den Details des Rezepts.
     *
     * @param recipe Das Rezept, dessen PDF generiert werden soll.
     * @return Das generierte PDF als Byte-Array.
     */
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

        return byteArrayOutputStream.toByteArray();  // Gibt das generierte PDF als Byte-Array zurück
    }

    /**
     * Parst die Zutaten eines Rezepts und gibt eine Liste der Zutatenobjekte zurück.
     *
     * @param ingredients Die Zutaten im String-Format.
     * @return Eine Liste von Ingredient-Objekten, die die Zutaten des Rezepts repräsentieren.
     */
    private List<Recipe.Ingredient> parseIngredients(String ingredients) {
        // Entferne die äußeren Klammern und splitte die Zutaten
        ingredients = ingredients.substring(1, ingredients.length() - 1);
        String[] ingredientsArray = ingredients.split(",");

        List<Recipe.Ingredient> parsedIngredients = new ArrayList<>();
        for (String ingredient : ingredientsArray) {
            ingredient = ingredient.trim();  // Entferne Leerzeichen
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
                name = ingredient;  // Kein Mengen- oder Einheitsteil vorhanden
            }

            parsedIngredients.add(new Recipe.Ingredient(amount, name, unit));  // Füge die Zutat zur Liste hinzu
        }

        return parsedIngredients;  // Gibt die Liste der geparsten Zutaten zurück
    }

    /**
     * Lädt alle Rezepte für einen bestimmten Benutzer aus der Datenbank.
     *
     * @param id Die Benutzer-ID, für den die Rezepte abgerufen werden.
     * @return Eine Liste der Rezepte des Benutzers.
     */
    public List<Recipe> getAllRecipesForUser(Long id) {
        return recipeDAO.loadAllRecipesForUser(id);  // Ruft alle Rezepte für den angegebenen Benutzer ab
    }
}
