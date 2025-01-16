package com.springboot.vitalorganize.service;

import com.springboot.vitalorganize.entity.IngredientEntity;
import com.springboot.vitalorganize.entity.ShoppingListItemEntity;
import com.springboot.vitalorganize.model.Shopping_List.ShoppingListData;
import com.springboot.vitalorganize.repository.IngredientRepository;
import com.springboot.vitalorganize.repository.ShoppingListItemRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@AllArgsConstructor
@Service
public class ShoppingListService {
    private final ShoppingListItemRepository shoppingListItemRepository;
    private final IngredientRepository ingredientRepository;
    private final TranslationService translationService;
    private final IngredientListService ingredientListService;

    public void checkIfIdExists(Long userId, Long itemId) {
        if(!shoppingListItemRepository.existsByUserIdAndIngredientId(userId, itemId)){
            throw new IllegalArgumentException("shoppingList.error.itemNotFound");
        }
    }

    public String extractAccessToken(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        } else {
            throw new IllegalArgumentException("authorizationHeader.wrongFormat");
        }
    }

    public void addItem(Long userId, String name) {
        ShoppingListItemEntity item = new ShoppingListItemEntity();
        item.setUserId(userId);
        Long ingredientId;

        // all ingredients are saved with their english name
        String englishName = translationService.translateQuery(name, "de", "en");

        IngredientEntity ingredient = ingredientRepository.findByUserIdAndName(userId, englishName).orElse(null);

        if(ingredient != null) {
            // ingredient exists in the ingredients table
            ingredientId = ingredient.getId();

            if(ingredient.isOnShoppingList()) {
                throw new IllegalArgumentException("shoppingList.error.alreadyOnList");
            }
        } else {
            // ingredient is not in the ingredients table
            ingredientListService.addIngredient(name);

            ingredientId = ingredientRepository.findByUserIdAndName(userId, name).orElseThrow().getId();
        }
        ingredientListService.toggleOnShoppingList(userId, ingredientId);
        item.setIngredientId(ingredientId);

        shoppingListItemRepository.save(item);
    }

    @Transactional
    public void deleteItem(Long userId, Long itemId) {
        checkIfIdExists(userId, itemId);

        ingredientListService.toggleOnShoppingList(userId, itemId);
        shoppingListItemRepository.deleteByUserIdAndIngredientId(userId, itemId);
    }

    public List<ShoppingListData> getAllItems(Long userId) {
        return shoppingListItemRepository.findShoppingListByUserId(userId);
    }

    public void updateAmount(Long userId, Long itemId, String newAmountStr){
        checkIfIdExists(userId, itemId);

        // get the shopping list item by id
        ShoppingListItemEntity item = shoppingListItemRepository.findByUserIdAndIngredientId(userId, itemId).orElseThrow();

        // input validation
        double newAmount;
        try {
            newAmount = Double.parseDouble(newAmountStr);

            if (newAmount <= 0) {
                throw new IllegalArgumentException("shoppingList.error.notGreaterThanZero");
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

    public Optional<ShoppingListData> getItem(Long userId, Long ingredientId) {
        return shoppingListItemRepository.findShoppingListItemByUserIdAndIngredientId(userId, ingredientId);
    }
}