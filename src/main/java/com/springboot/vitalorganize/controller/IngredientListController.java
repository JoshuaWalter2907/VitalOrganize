package com.springboot.vitalorganize.controller;

import com.springboot.vitalorganize.model.IngredientEntity;
import com.springboot.vitalorganize.repository.IngredientRepository;
import com.springboot.vitalorganize.service.IngredientListService;
import com.springboot.vitalorganize.service.ShoppingListService;
import com.springboot.vitalorganize.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@AllArgsConstructor
@RequestMapping("/ingredients")
public class IngredientListController {

    private final IngredientRepository ingredientRepository;
    private IngredientListService ingredientService;

    private UserService userService;
    private ShoppingListService shoppingListService;

    // loads the main ingredients page
    @GetMapping
    public String listIngredients(Model model,
                                  @AuthenticationPrincipal OAuth2User user,
                                  OAuth2AuthenticationToken token) {
        Long user_id  = userService.getCurrentUser(user, token).getId();

        List<IngredientEntity> ingredients = ingredientService.getAllIngredients(user_id);
        model.addAttribute("ingredients", ingredients);

        return "ingredients";
    }

    // add ingredient
    @PostMapping("/add")
    public String addIngredient(
            @RequestParam(value = "newIngredient") String name,
            RedirectAttributes redirectAttributes,
            @AuthenticationPrincipal OAuth2User user,
            OAuth2AuthenticationToken token) {
        Long user_id  = userService.getCurrentUser(user, token).getId();

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
    public String toggleOnShoppingList(@PathVariable("id") Long id,
                                       RedirectAttributes redirectAttributes,
                                       @AuthenticationPrincipal OAuth2User user,
                                       OAuth2AuthenticationToken token) {

        IngredientEntity ingredient = ingredientRepository.findById(id).orElseThrow();

        if(!ingredient.isOnShoppingList()){
            Long user_id  = userService.getCurrentUser(user, token).getId();
            shoppingListService.addItem(user_id, ingredient.getName(), redirectAttributes);
        } else {
            shoppingListService.deleteItem(id);
        }

        return "redirect:/ingredients";
    }


}