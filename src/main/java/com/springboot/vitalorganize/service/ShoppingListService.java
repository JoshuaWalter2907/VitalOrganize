package com.springboot.vitalorganize.service;

import com.springboot.vitalorganize.dto.ShoppingListData;
import com.springboot.vitalorganize.model.IngredientEntity;
import com.springboot.vitalorganize.model.IngredientRepository;
import com.springboot.vitalorganize.model.ShoppingListItemEntity;
import com.springboot.vitalorganize.model.ShoppingListItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Service
public class ShoppingListService {

    private final ShoppingListItemRepository shoppingListItemRepository;
    private final IngredientRepository ingredientRepository;

    private final IngredientListService ingredientListService;

    @Autowired
    public ShoppingListService(ShoppingListItemRepository shoppingListItemRepository,
                               IngredientListService ingredientListService,
                               IngredientRepository ingredientRepository) {
        this.shoppingListItemRepository = shoppingListItemRepository;
        this.ingredientListService = ingredientListService;
        this.ingredientRepository = ingredientRepository;
    }

    // add a shoppingList item
    public void addItem(
            Long userId,
            String name,
            RedirectAttributes redirectAttributes){
        ShoppingListItemEntity item = new ShoppingListItemEntity();
        item.setUserId(userId);

        Long ingredientId;
        IngredientEntity existingIngredient = ingredientRepository.findByUserIdAndName(userId, name);

        if(existingIngredient != null) {
            // ingredient already exists on the ingredient list of the user but not on the shopping list
            ingredientId = existingIngredient.getId();

            if(existingIngredient.isOnShoppingList()) {
                // ingredient already exists on both the ingredient and the shopping list
                redirectAttributes.addFlashAttribute("error", "shoppingList.error.alreadyOnList");
                return;
            }
        } else {
            // ingredient is neither on the ingredient nor the shopping list
            boolean success = ingredientListService.addIngredient(userId, name, redirectAttributes);

            // error when trying to add a new ingredient
            if(!success) {
                return;
            }

            // get the id of the new ingredient
            ingredientId = ingredientRepository.findByUserIdAndName(userId, name).getId();
        }
        // mark new ingredient as onShoppingList
        ingredientListService.toggleOnShoppingList(ingredientId);

        item.setIngredientId(ingredientId);

        shoppingListItemRepository.save(item);
    }

    public void deleteItem(Long id) {
        // keeps the item on the ingredient list but toggles onShoppingList status
        ingredientListService.toggleOnShoppingList(id);
        // deletes the item from the shoppingList
        shoppingListItemRepository.deleteById(id);
    }

    public List<ShoppingListData> getAllItems(Long userId) {
        List<ShoppingListData> items = shoppingListItemRepository.findShoppingListByUserId(userId);
        return items;
    }
}