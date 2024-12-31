package com.springboot.vitalorganize.controller;

import com.springboot.vitalorganize.dto.ShoppingListData;
import com.springboot.vitalorganize.model.IngredientEntity;
import com.springboot.vitalorganize.model.IngredientRepository;
import com.springboot.vitalorganize.model.ShoppingListItemEntity;
import com.springboot.vitalorganize.model.ShoppingListItemRepository;
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

@AllArgsConstructor
@Controller
@RequestMapping("/shoppingList")
public class ShoppingListController {

    private ShoppingListService shoppingListService;

    private ShoppingListItemRepository shoppingListItemRepository;
    private IngredientRepository ingredientRepository;
    private UserService userService;

    // loads the shoppingList page
    @GetMapping()
    public String listItems(Model model,
                            @AuthenticationPrincipal OAuth2User user,
                            OAuth2AuthenticationToken token) {
        Long user_id  = userService.getCurrentUser(user, token).getId();
        List<ShoppingListData> shoppingListItems = shoppingListService.getAllItems(user_id);

        double totalPrice = 0;

        // limit prices to 2 behind-the-comma-digits
        for(ShoppingListData shoppingListItem : shoppingListItems){
            shoppingListItem.setCalculatedPrice(Double.parseDouble(String.format("%.2f", shoppingListItem.getCalculatedPrice()).replace(",", ".")));
            totalPrice += shoppingListItem.getCalculatedPrice();
        }
        totalPrice = Double.parseDouble(String.format("%.2f", totalPrice).replace(",", "."));

        model.addAttribute("totalPrice", totalPrice);
        model.addAttribute("shoppingListItems", shoppingListItems);
        return "shoppingList";
    }

    // add ingredient
    @PostMapping("/add")
    public String addItem(
            @RequestParam(value = "ingredientName") String name,
            @AuthenticationPrincipal OAuth2User user,
            OAuth2AuthenticationToken token,
            RedirectAttributes redirectAttributes) {

        Long user_id  = userService.getCurrentUser(user, token).getId();

        shoppingListService.addItem(user_id, name, redirectAttributes);

        return "redirect:/shoppingList";
    }

    // delete ingredient
    @PostMapping("/delete/{id}")
    public String deleteItem(@PathVariable("id") Long id) {
        shoppingListService.deleteItem(id);
        return "redirect:/shoppingList";
    }

    @PostMapping("/editAmount/{id}")
    public String editAmount(
            @PathVariable("id") Long id,
            @RequestParam("newAmount") String newAmountStr,
            RedirectAttributes redirectAttributes) {

        // Retrieve the shopping list item by ID
        ShoppingListItemEntity item = shoppingListItemRepository.findById(id)
                .orElse(null);

        // input validation
        double newAmount;
        try {
            newAmount = Double.parseDouble(newAmountStr);

            if (newAmount <= 0) {
                redirectAttributes.addFlashAttribute("error", "shoppingList.error.notGreaterThanZero");
                return "redirect:/shoppingList";
            }
        } catch (NumberFormatException e) {
            // invalid input
            redirectAttributes.addFlashAttribute("error", "shoppingList.error.notANumber");
            return "redirect:/shoppingList";
        }

        // Update the item amount
        item.setPurchaseAmount(newAmount);

        IngredientEntity ingredient = ingredientRepository.findById(id).orElse(null);

        // get the standard price per 100g for example
        double price = ingredient.getPrice();
        double standardAmount = ingredient.getAmount();

        // total price for the new amount
        double newPrice = price / standardAmount * newAmount;

        item.setCalculatedPrice(newPrice);

        // Save the updated shopping list item to the repository
        shoppingListItemRepository.save(item);

        return "redirect:/shoppingList";
    }



}