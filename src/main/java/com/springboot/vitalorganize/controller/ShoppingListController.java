package com.springboot.vitalorganize.controller;

import com.springboot.vitalorganize.dto.ShoppingListData;
import com.springboot.vitalorganize.model.IngredientEntity;
import com.springboot.vitalorganize.model.IngredientRepository;
import com.springboot.vitalorganize.model.ShoppingListItemEntity;
import com.springboot.vitalorganize.model.ShoppingListItemRepository;
import com.springboot.vitalorganize.service.ShoppingListService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/shoppingList")
public class ShoppingListController {

    @Autowired
    private ShoppingListService shoppingListService;
    //TODO: ADJUST HTML TO THE NEW DTO, TEST TEST TEST, INSERT SAMPLE VALUES INTO THE SHOPPING LIST, DTO DOESNT WORK

    @Autowired
    private ShoppingListItemRepository shoppingListItemRepository;

    @Autowired
    private IngredientRepository ingredientRepository;

    // loads the shoppingList page
    @GetMapping()
    public String listItems(Model model,
                            HttpSession session) {
        Long user_id = (Long) session.getAttribute("user_id");
        List<ShoppingListData> shoppingListItems = shoppingListService.getAllItems(user_id);
        model.addAttribute("shoppingListItems", shoppingListItems);
        return "shoppingList";
    }

    // add ingredient
    @PostMapping("/add")
    public String addItem(
            @RequestParam(value = "ingredientName") String name,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Long user_id = (Long) session.getAttribute("user_id");

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

        // Save the updated shopping list item to the repository
        shoppingListItemRepository.save(item);

        IngredientEntity ingredient = ingredientRepository.findById(id).orElse(null);
        /*if (ingredient != null) {
            // get the standard price per 100g for example
            double price = ingredient.getPrice();
            double standardAmount = ingredient.getAmount();

            // total price for the new amount
            double totalPrice = price/standardAmount * newAmount;
            item.setCalculatedPrice(totalPrice);
            shoppingListItemRepository.save(item);
        }*/

        return "redirect:/shoppingList";
    }



}