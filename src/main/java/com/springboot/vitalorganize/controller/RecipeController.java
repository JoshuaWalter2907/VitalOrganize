package com.springboot.vitalorganize.controller;

import com.springboot.vitalorganize.model.Recipe;
import com.springboot.vitalorganize.service.MealService;
import com.springboot.vitalorganize.service.RecipeApiService;
import com.springboot.vitalorganize.service.RecipeService;
import com.springboot.vitalorganize.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpHeaders;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@Controller
public class RecipeController {

    @Autowired
    private RecipeService recipeService;

    @Autowired
    private UserService userService;

    @Autowired
    private MealService mealService;

    private final RecipeApiService recipeApiService;


    public RecipeController(RecipeApiService recipeApiService) {
        this.recipeApiService = recipeApiService;
    }

    /**
     * Zeigt die Seite zur Auswahl von Rezepten.
     */
    @GetMapping("/recipes")
    public String recipes() {
        return "chooseRecipes"; // Zeigt die Template-Datei für die Auswahl an
    }

    /**
     * Zeigt interne Rezepte basierend auf einer optionalen Suchanfrage.
     */
    @GetMapping("/recipes/internal")
    public String getInternalRecipes(@RequestParam(value = "search", required = false) String search, Model model) {

        List<Recipe> recipes;

        if (search != null && !search.isEmpty()) {
            // Suche nach Rezepten mit dem angegebenen Suchbegriff
            recipes = recipeService.searchRecipesByTitleForUser(search, userService.getCurrentUser().getId());
        } else {
            // Alle Rezepte des Benutzers ohne Suche zurückgeben
            recipes = recipeService.getAllRecipesForUser(userService.getCurrentUser().getId());
        }

        model.addAttribute("recipes", recipes); // Rezepte an die View übergeben
        model.addAttribute("isInternal", true);  // Kennzeichen für interne Rezepte setzen
        return "recipes"; // Zeigt die Template-Datei für Rezepte an
    }

    /**
     * Zeigt externe Rezepte basierend auf einer optionalen Suchanfrage.
     */
    @GetMapping("/recipes/external")
    public String getExternalRecipes(@RequestParam(value = "search", required = false) String search, Model model) {

        List<Recipe> recipes = Collections.emptyList(); // Leere Liste initialisieren

        if (search != null && !search.isEmpty()) {
            // Suche nach externen Rezepten über die API
            recipes = recipeApiService.searchRecipes(search);
        }

        model.addAttribute("recipes", recipes); // Externe Rezepte an die View übergeben
        model.addAttribute("isInternal", false);  // Kennzeichen für externe Rezepte setzen
        return "recipes"; // Zeigt die Template-Datei für Rezepte an
    }

    /**
     * Erstellt ein neues Rezept und leitet den Benutzer zur Bearbeitungsseite weiter.
     */
    @PostMapping("/recipes/new")
    public String createRecipe(@RequestParam("recipeName") String recipeName, Model model) {
        // Neues Rezept erstellen und die ID zurückgeben
        int recipeId = recipeService.createEmptyRecipe(recipeName, userService.getCurrentUser().getId());
        // Weiterleitung zur Bearbeitungsseite des neuen Rezepts
        return "redirect:/recipes/edit/" + recipeId;
    }

    /**
     * Zeigt die Bearbeitungsseite für ein Rezept an.
     */
    @GetMapping("/recipes/edit/{id}")
    public String editRecipe(@PathVariable("id") int id, Model model) {
        // Rezept mit der angegebenen ID laden
        Recipe recipe = recipeService.getRecipeById(id);
        model.addAttribute("recipe", recipe); // Rezept an die View übergeben
        return "editRecipe"; // Zeigt die Template-Datei für die Bearbeitung an
    }

    /**
     * Speichert ein Rezept mit den angegebenen Details.
     */
    @PostMapping("/recipes/saveLocal")
    public String saveRecipe(
            @RequestParam("title") String title,
            @RequestParam("difficulty") String difficulty,
            @RequestParam("portions") int portions,
            @RequestParam("ingredients") String ingredients,
            @RequestParam("calories") Integer calories,
            @RequestParam("rating") Double rating,
            @RequestParam("source") String source,
            @RequestParam("time") Double time) {

        // Rezept im System speichern
        recipeService.saveRecipe(
                title, difficulty, portions, ingredients, calories, rating, source, time,
                userService.getCurrentUser().getId());

        return "redirect:/recipes/internal"; // Weiterleitung zur Seite mit internen Rezepten
    }

    /**
     * Markiert ein Rezept als gekocht und fügt es zu den Mahlzeiten hinzu.
     */
    @PostMapping("/recipes/cooked")
    public String cookRecipe(@RequestParam("id") Long recipeId, Model model) {
        mealService.addMeal(userService.getCurrentUser().getId(), recipeId, LocalDate.now()); // Mahlzeit hinzufügen
        return "redirect:/recipes/internal"; // Weiterleitung zur Seite mit internen Rezepten
    }

    /**
     * Löscht ein Rezept basierend auf der angegebenen ID.
     */
    @DeleteMapping("/recipes/delete/{id}")
    public ResponseEntity<Void> deleteRecipe(@PathVariable Long id) {
        try {
            recipeService.deleteById(id); // Rezept aus der Datenbank löschen
            return ResponseEntity.ok().build(); // Erfolg zurückgeben
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); // Fehler zurückgeben
        }
    }

    /**
     * Speichert ein Rezept, das per API übergeben wird.
     */
    @PostMapping("/recipes")
    public ResponseEntity<String> saveRecipe(@RequestBody Recipe recipe) {
        recipeService.updateRecipe(recipe); // Rezept aktualisieren
        System.out.println("Rezept gespeichert: " + recipe.getTitle());
        return ResponseEntity.ok("Rezept gespeichert!"); // Bestätigung zurückgeben
    }

    /**
     * Exportiert ein Rezept als PDF.
     */
    @GetMapping("/recipes/{id}/export")
    public ResponseEntity<byte[]> exportRecipeAsPdf(@PathVariable int id) {
        Recipe recipe = recipeService.getRecipeById(id); // Rezept laden
        byte[] pdfBytes = recipeService.generateRecipePdf(recipe); // PDF generieren

        // PDF-Datei als Download zurückgeben
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=recipe_" + id + ".pdf")
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

}
