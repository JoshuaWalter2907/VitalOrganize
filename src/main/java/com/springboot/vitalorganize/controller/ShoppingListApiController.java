package com.springboot.vitalorganize.controller;

import com.springboot.vitalorganize.entity.Profile_User.UserEntity;
import com.springboot.vitalorganize.model.ShoppingListData;
import com.springboot.vitalorganize.repository.UserRepository;
import com.springboot.vitalorganize.service.ShoppingListService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shoppingList")
@AllArgsConstructor
public class ShoppingListApiController {

    private final ShoppingListService shoppingListService;
    private final UserRepository userRepository;

    // fetch the whole shopping list
    @GetMapping("/all")
    public ResponseEntity<?> getAllItems(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) {
        try {
            String token = shoppingListService.extractAccessToken(authorizationHeader);
            UserEntity userEntity = userRepository.findByToken(token);
            Long userId = userEntity.getId();

            List<ShoppingListData> shoppingListItems = shoppingListService.getAllItems(userId);

            return ResponseEntity.ok(shoppingListItems);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("An error occurred while fetching the shopping list (" + e.getMessage() + ").");
        }
    }

    // add an item to the shopping list
    @PostMapping
    public ResponseEntity<?> addItem(@RequestParam("ingredientName") String name,
                                     @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) {
        try {
            String token = shoppingListService.extractAccessToken(authorizationHeader);
            UserEntity userEntity = userRepository.findByToken(token);
            Long userId = userEntity.getId();

            shoppingListService.addItem(userId, name);

            return ResponseEntity.ok("Item added successfully.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("An error occurred while adding the item (" + e.getMessage() + ").");
        }
    }

    // delete the item from the shopping list
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteItem(@PathVariable Long id,
                                        @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) {
        try {
            String token = shoppingListService.extractAccessToken(authorizationHeader);
            UserEntity userEntity = userRepository.findByToken(token);
            Long userId = userEntity.getId();

            shoppingListService.deleteItem(userId, id);

            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("An error occurred while deleting the item (" + e.getMessage() + ").");
        }
    }

    // update an item's amount in the shopping list
    @PutMapping("/{id}/amount")
    public ResponseEntity<?> updateItemAmount(@PathVariable Long id,
                                              @RequestParam("newAmount") String newAmountStr,
                                              @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) {
        try {
            String token = shoppingListService.extractAccessToken(authorizationHeader);
            UserEntity userEntity = userRepository.findByToken(token);
            Long userId = userEntity.getId();

            shoppingListService.updateAmount(userId, id, newAmountStr);

            return ResponseEntity.ok("Item updated successfully.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("An error occurred while updating the item amount (" + e.getMessage() + ").");
        }
    }

    // fetch a single item from the shopping list
    @GetMapping("/{id}")
    public ResponseEntity<?> getItem(@PathVariable Long id,
                                     @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) {
        try {
            String token = shoppingListService.extractAccessToken(authorizationHeader);
            UserEntity userEntity = userRepository.findByToken(token);
            Long userId = userEntity.getId();

            shoppingListService.checkIfIdExists(userId, id);

            ShoppingListData data = shoppingListService.getItem(userId, id).orElse(null);

            return ResponseEntity.ok(data);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("An error occurred while fetching the item (" + e.getMessage() + ").");
        }
    }
}