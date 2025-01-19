package com.springboot.vitalorganize.service;

import com.springboot.vitalorganize.entity.IngredientEntity;
import com.springboot.vitalorganize.entity.ShoppingListItemEntity;
import com.springboot.vitalorganize.model.ShoppingListData;
import com.springboot.vitalorganize.repository.IngredientRepository;
import com.springboot.vitalorganize.repository.ShoppingListItemRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Dieser Service ist zuständig für die Funktionalität der Einkaufslisten-Seite
 * Diese Klasse bietet Methoden für die Einkaufsliste
 */
@AllArgsConstructor
@Service
public class ShoppingListService {
    private final ShoppingListItemRepository shoppingListItemRepository;
    private final IngredientRepository ingredientRepository;
    private final TranslationService translationService;
    private final IngredientListService ingredientListService;

    /**
     * Prüft, ob die Id einer Zutat auf der Einkaufsliste des Nutzers vorhanden ist
     * @param userId Die Id des Nutzers
     * @param itemId Die Id der Zutat
     * @throws IllegalArgumentException wenn die Id nicht gefunden wird
     */
    public void checkIfIdExists(Long userId, Long itemId) {
        if(!shoppingListItemRepository.existsByUserIdAndIngredientId(userId, itemId)){
            throw new IllegalArgumentException("shoppingList.error.itemNotFound");
        }
    }

    /**
     * Gibt das Access Token aus dem AuthorizationHeader zurück
     * @param authorizationHeader Der AuthorizationHeader mit dem Access Token
     * @return Access Token
     * @throws IllegalArgumentException wenn der AuthorizationHeader das falsche Format hat
     */
    public String extractAccessToken(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        } else {
            throw new IllegalArgumentException("authorizationHeader.wrongFormat");
        }
    }
    /**
     * Gibt das Access Token aus dem AuthorizationHeader zurück
     * @param userId Die Id des Nutzers
     * @param germanOrEnglishName Der deutsche oder englische Name der Zutat
     * @throws IllegalArgumentException wenn die Zutat schon auf der Einkaufsliste ist
     */
    public void addItem(Long userId, String germanOrEnglishName) {
        ShoppingListItemEntity item = new ShoppingListItemEntity();
        item.setUserId(userId);
        Long ingredientId;

        // all ingredients are saved with their english name
        String name = translationService.translateQuery(germanOrEnglishName, "de", "en");

        IngredientEntity ingredient = ingredientRepository.findByUserIdAndName(userId, name).orElse(null);

        if(ingredient != null) {
            // ingredient exists in the ingredients table
            ingredientId = ingredient.getId();

            if(ingredient.isOnShoppingList()) {
                throw new IllegalArgumentException("shoppingList.error.alreadyOnList");
            }
        } else {
            // ingredient is not in the ingredients table
            ingredientListService.addIngredient(userId, name);

            ingredientId = ingredientRepository.findByUserIdAndName(userId, name).orElseThrow().getId();
        }
        ingredientListService.toggleOnShoppingList(userId, ingredientId);
        item.setIngredientId(ingredientId);

        shoppingListItemRepository.save(item);
    }

    /**
     * Löscht eine Zutat von der Einkaufsliste
     * @param userId Die Id des Nutzers
     * @param itemId Die Id der Zutat
     */
    @Transactional
    public void deleteItem(Long userId, Long itemId) {
        checkIfIdExists(userId, itemId);

        ingredientListService.toggleOnShoppingList(userId, itemId);
        shoppingListItemRepository.deleteByUserIdAndIngredientId(userId, itemId);
    }

    /**
     * Gibt die ganze Einkaufsliste des Nutzers zurück
     * @param userId Die Id des Nutzers
     * @return Liste aller Einkaufslisteneinträge
     */
    public List<ShoppingListData> getAllItems(Long userId) {
        return shoppingListItemRepository.findShoppingListByUserId(userId);
    }

    /**
     * Verändert die Menge einer Zutat
     * @param userId Die Id des Nutzers
     * @param itemId Die Id der Zutat
     * @param newAmountStr Die neue Menge als String
     */
    public void updateAmount(Long userId, Long itemId, String newAmountStr){
        checkIfIdExists(userId, itemId);

        // get the shopping list item by id
        ShoppingListItemEntity item = shoppingListItemRepository.findByUserIdAndIngredientId(userId, itemId).orElseThrow();

        // input validation
        double newAmount;
        try {
            newAmount = Double.parseDouble(newAmountStr);

            if (newAmount < 0) {
                throw new IllegalArgumentException("shoppingList.error.notAPositiveNumber");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("shoppingList.error.notANumber");
        }

        // at this point the item and ingredient can't be null
        item.setPurchaseAmount(newAmount);

        IngredientEntity ingredient = ingredientRepository.findByUserIdAndId(userId, itemId).orElseThrow();

        // get the price (per 100g)
        double price = ingredient.getPrice();

        // total price for the new amount
        double newPrice = price/100 * newAmount;
        item.setCalculatedPrice(newPrice);

        shoppingListItemRepository.save(item);
    }

    /**
     * Gibt eine einzelne Zutat von der Einkaufsliste zurück
     * @param userId Die Id des Nutzers
     * @param itemId Die Id der Zutat
     * @return Einzelner Einkaufslisteneintrag
     */
    public Optional<ShoppingListData> getItem(Long userId, Long itemId) {
        return shoppingListItemRepository.findShoppingListItemByUserIdAndIngredientId(userId, itemId);
    }
}