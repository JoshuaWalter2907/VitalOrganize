package com.springboot.vitalorganize.controller;

import com.springboot.vitalorganize.model.Recipe;
import com.springboot.vitalorganize.service.RecipeApiService;
import com.springboot.vitalorganize.service.RecipeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpHeaders;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Controller
public class RecipeController {

    @Autowired
    private RecipeService recipeService;

    private final RecipeApiService recipeApiService;

    public RecipeController(RecipeApiService recipeApiService) {
        this.recipeApiService = recipeApiService;
    }

    @GetMapping("/recipes")
    public String recipes() {
        return "chooseRecipes";
    }

    @GetMapping("/recipes/internal")
    public String getInternalRecipes(@RequestParam(value = "search", required = false) String search, Model model) {

        List<Recipe> recipes;

        if (search != null && !search.isEmpty()) {
            // Führe die Suche mit dem angegebenen Schlagwort durch
            recipes = recipeService.searchRecipesByTitle(search);
        } else {
            // Keine Suche, gib alle Rezepte zurück
            recipes = recipeService.getAllRecipes();
        }

        model.addAttribute("recipes", recipes);
        model.addAttribute("isInternal", true);  // Setze die Variable für interne Rezepte
        return "recipes"; // Deine Template-Datei
    }

    @GetMapping("/recipes/external")
    public String getExternalRecipes(@RequestParam(value = "search", required = false) String search, Model model) {

        List<Recipe> recipes = Collections.emptyList();

        if (search != null && !search.isEmpty()) {
            // Führe die Suche mit dem angegebenen Schlagwort durch
            recipes = recipeApiService.searchRecipes(search);
        }

        model.addAttribute("recipes", recipes);
        model.addAttribute("isInternal", false);  // Setze die Variable für interne Rezepte
        return "recipes"; // Deine Template-Datei
    }

    @PostMapping("/recipes/new")
    public String createRecipe(@RequestParam("recipeName") String recipeName, Model model) {
        // 1. Rezept in der Datenbank speichern
        int recipeId = recipeService.createEmptyRecipe(recipeName);
        // 2. Zur Seite für weitere Eingaben weiterleiten
        return "redirect:/recipes/edit/" + recipeId;
    }

    @GetMapping("/recipes/edit/{id}")
    public String editRecipe(@PathVariable("id") int id, Model model) {
        // Rezeptdaten optional laden, falls benötigt
        Recipe recipe = recipeService.getRecipeById(id);
        model.addAttribute("recipe", recipe);
        return "editRecipe";
    }

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

        recipeService.saveRecipe(
                title, difficulty, portions, ingredients, calories, rating, source, time
        );

        return "redirect:/recipes/internal";
    }


    @DeleteMapping("/recipes/delete/{id}")
    public ResponseEntity<Void> deleteRecipe(@PathVariable Long id) {
        try {
            recipeService.deleteById(id); // Methode im Service, die das Rezept löscht
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/recipes")
    public ResponseEntity<String> saveRecipe(@RequestBody Recipe recipe) {
        recipeService.updateRecipe(recipe);
        System.out.println("Rezept gespeichert: " + recipe.getTitle());
        return ResponseEntity.ok("Rezept gespeichert!");
    }

    @GetMapping("/recipes/{id}/export")
    public ResponseEntity<byte[]> exportRecipeAsPdf(@PathVariable int id) {
        Recipe recipe = recipeService.getRecipeById(id);
        byte[] pdfBytes = recipeService.generateRecipePdf(recipe);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=recipe_" + id + ".pdf")
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

}
