package com.springboot.vitalorganize.controller;

import com.springboot.vitalorganize.dto.ShoppingListData;
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

    private final ShoppingListService shoppingListService;
    private final UserService userService;

    // loads the shoppingList page
    @GetMapping()
    public String listItems(Model model,
                            @AuthenticationPrincipal OAuth2User user,
                            OAuth2AuthenticationToken token) {
        Long user_id  = userService.getCurrentUser(user, token).getId();

        List<ShoppingListData> shoppingListItems = shoppingListService.getAllItems(user_id);

        // limit display prices to 2 behind-the-comma-digits
        double totalPrice = 0;
        for(ShoppingListData shoppingListItem : shoppingListItems){
            shoppingListItem.setCalculatedPrice(Double.parseDouble(String.format("%.2f", shoppingListItem.getCalculatedPrice()).replace(",", ".")));
            totalPrice += shoppingListItem.getCalculatedPrice();
        }
        totalPrice = Double.parseDouble(String.format("%.2f", totalPrice).replace(",", "."));

        model.addAttribute("totalPrice", totalPrice);
        model.addAttribute("shoppingListItems", shoppingListItems);
        return "shoppingList/shoppingList";
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
    public String deleteItem(@PathVariable("id") Long id,
                             @AuthenticationPrincipal OAuth2User user,
                             OAuth2AuthenticationToken token) {
        Long userId = userService.getCurrentUser(user, token).getId();

        shoppingListService.deleteItem(userId, id);
        return "redirect:/shoppingList";
    }

    @PostMapping("/updateAmount/{id}")
    public String updateAmount(
            @PathVariable("id") Long id,
            @RequestParam("newAmount") String newAmountStr,
            RedirectAttributes redirectAttributes,
            @AuthenticationPrincipal OAuth2User user,
            OAuth2AuthenticationToken token) {
        Long userId = userService.getCurrentUser(user, token).getId();

        String error = shoppingListService.updateAmount(userId, id, newAmountStr);
        if(!error.isEmpty()){
            redirectAttributes.addFlashAttribute("error", error);
        }
        return "redirect:/shoppingList";
    }



}