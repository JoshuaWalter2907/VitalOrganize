package com.springboot.vitalorganize.controller;

import com.springboot.vitalorganize.model.IngredientEntity;
import com.springboot.vitalorganize.service.IngredientListService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;

@Controller
@RequestMapping("/ingredients")
public class IngredientListController {

    @Autowired
    private IngredientListService ingredientService;

    @Qualifier("messageSource")
    @Autowired
    private MessageSource messageSource;

    // loads the main ingredients page
    @GetMapping
    public String listIngredients(Model model,
                                  HttpSession session) {
        Long user_id = (Long) session.getAttribute("user_id");
        List<IngredientEntity> ingredients = ingredientService.getAllIngredients(user_id);
        model.addAttribute("ingredients", ingredients);

        return "ingredients";
    }

    // add ingredient
    @PostMapping("/add")
    public String addIngredient(
            @RequestParam(value = "newIngredient") String name,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        Long user_id = (Long) session.getAttribute("user_id");
        ingredientService.addIngredient(user_id, name, redirectAttributes);

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

    // toggles the onShoppingList status
    @PostMapping("/onShoppingList/{id}")
    public String toggleOnShoppingList(@PathVariable("id") Long id) {
        ingredientService.toggleOnShoppingList(id);
        return "redirect:/ingredients";
    }


}