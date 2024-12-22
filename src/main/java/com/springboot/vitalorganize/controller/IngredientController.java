package com.springboot.vitalorganize.controller;

import com.springboot.vitalorganize.model.IngredientEntity;
import com.springboot.vitalorganize.model.IngredientRepository;
import com.springboot.vitalorganize.service.IngredientService;
import com.springboot.vitalorganize.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/ingredients")
public class IngredientController {

    @Autowired
    private IngredientService ingredientService;

    // loads the main ingredients page
    @GetMapping
    public String listIngredients(Model model) {
        List<IngredientEntity> ingredients = ingredientService.getAllIngredients();
        model.addAttribute("ingredients", ingredients);

        return "ingredients";
    }

    // add ingredient
    @PostMapping("/add")
    public String addIngredient(@RequestParam(value = "newIngredient") String name) {
        ingredientService.addIngredient(name);
        System.out.println("gemacht1");
        return "redirect:/ingredients";
    }

    // delete ingredient
    @PostMapping("/delete/{id}")
    public String deleteIngredient(@PathVariable("id") Long id) {
        ingredientService.deleteIngredient(id);
        return "redirect:/ingredients";
    }

    // toggles the favourite status
    @PostMapping("/favourite/{id}")
    public String toggleFavouriteIngredient(@PathVariable("id") Long id) {
        ingredientService.toggleFavourite(id);
        return "redirect:/ingredients";
    }


}