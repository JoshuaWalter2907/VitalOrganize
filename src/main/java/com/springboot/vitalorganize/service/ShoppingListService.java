package com.springboot.vitalorganize.service;

import com.springboot.vitalorganize.dto.ShoppingListData;
import com.springboot.vitalorganize.model.IngredientEntity;
import com.springboot.vitalorganize.repository.IngredientRepository;
import com.springboot.vitalorganize.model.ShoppingListItemEntity;
import com.springboot.vitalorganize.repository.ShoppingListItemRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@AllArgsConstructor
@Service
public class ShoppingListService {
    private final ShoppingListItemRepository shoppingListItemRepository;
    private final IngredientRepository ingredientRepository;
    private final TranslationService translationService;
    private final IngredientListService ingredientListService;

    public boolean checkIfIdExists(Long userId, Long itemId) {
        return shoppingListItemRepository.existsByUserIdAndItemId(userId, itemId);
    }

    public void addItem(
            Long userId,
            String name,
            RedirectAttributes redirectAttributes) {
        ShoppingListItemEntity item = new ShoppingListItemEntity();
        item.setUserId(userId);

        Long ingredientId;
        IngredientEntity existingIngredient = ingredientRepository.findByUserIdAndName(userId, name);

        if(existingIngredient == null){
            // try again with translated ingredient name
            name = translationService.translateQuery(name, "de", "en");
            existingIngredient = ingredientRepository.findByUserIdAndName(userId, name);
            if(name.startsWith("Translation failed:")){
                if (redirectAttributes != null) {
                    redirectAttributes.addFlashAttribute("error", "shoppingList.error.translationFailed");
                    return;
                } else {
                    throw new IllegalArgumentException("Translation failed for the ingredient name.");
                }
            }
        }

        if(existingIngredient != null) {
            ingredientId = existingIngredient.getId();

            if(existingIngredient.isOnShoppingList()) {
                if (redirectAttributes != null) {
                    redirectAttributes.addFlashAttribute("error", "shoppingList.error.alreadyOnList");
                    return;
                } else {
                    throw new IllegalArgumentException("The ingredient is already on the shopping list.");
                }
            }
        } else {
            boolean success = ingredientListService.addIngredient(userId, name, redirectAttributes);

            if (!success) {
                if (redirectAttributes == null) {
                    throw new IllegalArgumentException("Failed to add the ingredient.");
                }
                return;
            }

            ingredientId = ingredientRepository.findByUserIdAndName(userId, name).getId();
        }
        ingredientListService.toggleOnShoppingList(userId, ingredientId);
        item.setIngredientId(ingredientId);

        shoppingListItemRepository.save(item);
    }

    @Transactional
    public void deleteItem(Long userId, Long itemId) {
        if (!checkIfIdExists(userId, itemId)) {
            throw new IllegalArgumentException("Item with ID " + itemId + " not found.");
        }
        ingredientListService.toggleOnShoppingList(userId, itemId);
        shoppingListItemRepository.deleteByUserIdAndItemId(userId, itemId);
    }

    public List<ShoppingListData> getAllItems(Long userId) {
        return shoppingListItemRepository.findShoppingListByUserId(userId);
    }

    public String updateAmount(Long userId, Long itemId, String newAmountStr){
        if (!checkIfIdExists(userId, itemId)) {
            return "shoppingList.error.itemNotFound";
        }

        // Retrieve the shopping list item by ID
        ShoppingListItemEntity item = shoppingListItemRepository.findByUserIdAndIngredientId(userId, itemId)
                .orElse(null);

        // input validation
        double newAmount;
        try {
            newAmount = Double.parseDouble(newAmountStr);

            if (newAmount <= 0) {
                return "shoppingList.error.notGreaterThanZero";
            }
        } catch (NumberFormatException e) {
            // invalid input
            return "shoppingList.error.notANumber";
        }

        // Update the item amount
        item.setPurchaseAmount(newAmount);

        IngredientEntity ingredient = ingredientRepository.findByUserIdAndIngredientId(userId, itemId).orElse(null);

        // get the standard price per 100g
        double price = ingredient.getPrice();
        double standardAmount = ingredient.getAmount();

        // total price for the new amount
        double newPrice = price / standardAmount * newAmount;

        item.setCalculatedPrice(newPrice);

        // Save the updated shopping list item to the repository
        shoppingListItemRepository.save(item);

        // no error
        return "";
    }

    public Optional<ShoppingListData> getItem(Long userId, Long ingredientId) {
        return shoppingListItemRepository.findShoppingListItemByUserIdAndIngredientId(userId, ingredientId);
    }
}