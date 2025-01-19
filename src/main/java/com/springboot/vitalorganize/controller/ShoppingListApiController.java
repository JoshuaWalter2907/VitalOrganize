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

/**
 * Controller für die ShoppingList-API
 */
@RestController
@RequestMapping("/api/shoppingList")
@AllArgsConstructor
public class ShoppingListApiController {

    private final ShoppingListService shoppingListService;
    private final UserRepository userRepository;

    /**
     * Gibt die gesamte Einkaufsliste des Nutzers zurück
     * @param authorizationHeader Der Authorization-Header mit API-Token
     * @return API-Response
     */
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

    /**
     * Fügt der Einkaufsliste eine Zutat hinzu
     * @param name Der Name der Zutat
     * @param authorizationHeader Der Authorization-Header mit API-Token
     * @return API-Response
     */
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

    /**
     * Löscht eine Zutat von der Einkaufsliste
     * @param id Die Id der Zutat
     * @param authorizationHeader Der Authorization-Header mit API-Token
     * @return API-Response
     */
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

    /**
     * Verändert die Menge einer Zutat
     * @param id Die Id der Zutat
     * @param newAmountStr Die neue Menge als String
     * @param authorizationHeader Der Authorization-Header mit API-Token
     * @return API-Response
     */
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

    /**
     * Gibt eine einzelne Zutat zurück
     * @param id Die Id der Zutat
     * @param authorizationHeader Der Authorization-Header mit API-Token
     * @return API-Response
     */
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