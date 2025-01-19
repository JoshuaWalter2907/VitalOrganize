package com.springboot.vitalorganize.controller;

import com.springboot.vitalorganize.model.ShoppingListData;
import com.springboot.vitalorganize.service.ShoppingListService;
import com.springboot.vitalorganize.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Controller für die ShoppingList-Page
 */
@AllArgsConstructor
@Controller
@RequestMapping("/shoppingList")
public class ShoppingListController {

    private final ShoppingListService shoppingListService;
    private final UserService userService;

    /**
     * Zeigt die Einkaufsliste des Nutzers an
     * @param model Das Model für die View
     * @return ShoppingList-Page
     */
    @GetMapping()
    public String listItems(Model model) {
        Long userId = userService.getCurrentUser().getId();

        List<ShoppingListData> shoppingListItems = shoppingListService.getAllItems(userId);

        // calculate the total shopping list price
        double totalPrice = 0;
        for(ShoppingListData shoppingListItem : shoppingListItems){
            totalPrice += shoppingListItem.getCalculatedPriceInEuros();
        }

        model.addAttribute("shoppingListItems", shoppingListItems);
        model.addAttribute("totalPrice", totalPrice);
        return "shoppingList/shoppingList";
    }

    /**
     * Fügt der Einkaufsliste eine Zutat hinzu
     * @param name Der Name der Zutat
     * @param attr Wird verwendet, um Fehlermeldungen an die View zu übermitteln
     * @return ShoppingList-Page
     */
    @PostMapping("/add")
    public String addItem(@RequestParam(value = "ingredientName") String name,
                          RedirectAttributes attr) {
        Long userId = userService.getCurrentUser().getId();

        try{
            shoppingListService.addItem(userId, name);
        } catch (IllegalArgumentException e){
            attr.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/shoppingList";
    }

    /**
     * Löscht eine Zutat von der Einkaufsliste
     * @param id Die Id der Zutat
     * @param attr Wird verwendet, um Fehlermeldungen an die View zu übermitteln
     * @return ShoppingList-Page
     */
    @PostMapping("/delete/{id}")
    public String deleteItem(@PathVariable("id") Long id,
                             RedirectAttributes attr) {
        Long userId = userService.getCurrentUser().getId();

        try{
            shoppingListService.deleteItem(userId, id);
        } catch (IllegalArgumentException e) {
            attr.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/shoppingList";
    }

    /**
     * Verändert die Menge einer Zutat
     * @param id Die Id der Zutat
     * @param newAmountStr Die neue Menge als String
     * @param attr Wird verwendet, um Fehlermeldungen an die View zu übermitteln
     * @return ShoppingList-Page
     */
    @PostMapping("/updateAmount/{id}")
    public String updateAmount(@PathVariable("id") Long id,
                               @RequestParam("newAmount") String newAmountStr,
                               RedirectAttributes attr) {
        Long userId = userService.getCurrentUser().getId();

        try{
            shoppingListService.updateAmount(userId, id, newAmountStr);
        } catch (IllegalArgumentException e) {
            attr.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/shoppingList";
    }
}