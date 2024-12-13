package com.springboot.vitalorganize.controller;

import com.springboot.vitalorganize.dao.RecipeDAO;
import com.springboot.vitalorganize.model.Recipe;
import com.springboot.vitalorganize.service.RecipeApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collections;
import java.util.List;

@Controller
public class RecipeController {

    @Autowired
    RecipeDAO recipeDAO;

    private final RecipeApiService recipeApiService;

    public RecipeController(RecipeApiService recipeApiService) {
        this.recipeApiService = recipeApiService;
    }

    @GetMapping("/recipes")
    public String recipes() {
        return "chooseRecipes";
    }
    /**
    @GetMapping("/recipes/search")
    public String searchRecipes(@RequestParam String query, Model model) {
        model.addAttribute("recipes", recipeApiService.searchRecipes(query));
        return "externalRecipes";
    }
    */
    @GetMapping("/recipes/internal")
    public String getInternalRecipes(@RequestParam(value = "search", required = false) String search, Model model) {

        List<Recipe> recipes;

        if (search != null && !search.isEmpty()) {
            // Führe die Suche mit dem angegebenen Schlagwort durch
            recipes = recipeDAO.loadRecipe(search);
        } else {
            // Keine Suche, gib alle Rezepte zurück
            recipes = recipeDAO.loadAllRecipes();
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

}
