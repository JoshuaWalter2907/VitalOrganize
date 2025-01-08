package com.springboot.vitalorganize.controller;

import com.springboot.vitalorganize.dto.ShoppingListData;
import com.springboot.vitalorganize.service.ShoppingListService;
import com.springboot.vitalorganize.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shoppingList")
@AllArgsConstructor
public class ShoppingListApiController {

    private final ShoppingListService shoppingListService;
    private final UserService userService;

    // Return the whole shopping list
    @GetMapping("/all")
    public ResponseEntity<?> getAllItems(@AuthenticationPrincipal OAuth2User user,
                                         OAuth2AuthenticationToken token) {
        try {
            Long userId = userService.getCurrentUser(user, token).getId();
            List<ShoppingListData> shoppingListItems = shoppingListService.getAllItems(userId);

            // Limit prices to 2 decimal places
            shoppingListItems.forEach(item -> item.setCalculatedPrice(
                    Double.parseDouble(String.format("%.2f", item.getCalculatedPrice()).replace(",", "."))
            ));

            return ResponseEntity.ok(shoppingListItems);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("An error occurred while fetching the shopping list.");
        }
    }

    // Add an item to the shopping list
    @PostMapping
    public ResponseEntity<?> addItem(@RequestParam("ingredientName") String name,
                                     @AuthenticationPrincipal OAuth2User user,
                                     OAuth2AuthenticationToken token) {
        try {
            Long userId = userService.getCurrentUser(user, token).getId();

            // redirectAttributes null, so the service can give an api adjusted error response
            shoppingListService.addItem(userId, name, null);

            return ResponseEntity.ok("Item added successfully.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("An error occurred while adding the item.");
        }
    }

    // Delete an item from the shopping list
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteItem(@PathVariable Long id,
                                        @AuthenticationPrincipal OAuth2User user,
                                        OAuth2AuthenticationToken token) {
        try {
            Long userId = userService.getCurrentUser(user, token).getId();
            shoppingListService.deleteItem(userId, id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("An error occurred while deleting the item.");
        }
    }

    // Update an item amount in the shopping list
    @PutMapping("/{id}/amount")
    public ResponseEntity<?> updateItemAmount(@PathVariable Long id,
                                              @RequestParam("newAmount") String newAmountStr,
                                              @AuthenticationPrincipal OAuth2User user,
                                              OAuth2AuthenticationToken token) {
        try {
            Long userId = userService.getCurrentUser(user, token).getId();
            String error = shoppingListService.updateAmount(userId, id, newAmountStr);

            if (!error.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }

            return ResponseEntity.ok("Item updated successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("An error occurred while updating the item amount.");
        }
    }

    // Return an item from the shopping list
    @GetMapping("/{id}")
    public ResponseEntity<?> getItem(@PathVariable Long id,
                                     @AuthenticationPrincipal OAuth2User user,
                                     OAuth2AuthenticationToken token) {
        try {
            Long userId = userService.getCurrentUser(user, token).getId();

            if (!shoppingListService.checkIfIdExists(userId, id)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Item with ID " + id + " not found.");
            }

            ShoppingListData data = shoppingListService.getItem(userId, id).orElseThrow();
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("An error occurred while fetching the item.");
        }
    }
}
