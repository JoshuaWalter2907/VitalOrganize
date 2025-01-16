package com.springboot.vitalorganize.controller;

import com.springboot.vitalorganize.model.Shopping_List.ShoppingListData;
import com.springboot.vitalorganize.service.ShoppingListService;
import com.springboot.vitalorganize.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@AllArgsConstructor
@Controller
@RequestMapping("/shoppingList")
public class ShoppingListController {

    private final ShoppingListService shoppingListService;
    private final UserService userService;

    // load the shoppingList page
    @GetMapping()
    public String listItems(Model model) {
        Long userId = userService.getCurrentUser().getId();

        List<ShoppingListData> shoppingListItems = shoppingListService.getAllItems(userId);

        // calculate the total shopping cost and limit all prices to 2 displayed digits behind the comma
        double totalPrice = 0;
        for(ShoppingListData shoppingListItem : shoppingListItems){
            shoppingListItem.setCalculatedPriceInEuros(Double.parseDouble(String.format("%.2f", shoppingListItem.getCalculatedPriceInEuros()).replace(",", ".")));
            totalPrice += shoppingListItem.getCalculatedPriceInEuros();
        }
        totalPrice = Double.parseDouble(String.format("%.2f", totalPrice).replace(",", "."));

        model.addAttribute("shoppingListItems", shoppingListItems);
        model.addAttribute("totalPrice", totalPrice);
        return "shoppingList/shoppingList";
    }

    // add an item to the shopping list
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

    // delete the item from the shopping list
    @PostMapping("/delete/{id}")
    public String deleteItem(@PathVariable("id") Long itemId,
                             RedirectAttributes attr) {
        Long userId = userService.getCurrentUser().getId();

        try{
            shoppingListService.deleteItem(userId, itemId);
        } catch (IllegalArgumentException e) {
            attr.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/shoppingList";
    }

    // update an item's amount in the shopping list
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