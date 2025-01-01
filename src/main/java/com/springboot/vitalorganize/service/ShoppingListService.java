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

    public void addItem(
            Long userId,
            String name,
            RedirectAttributes redirectAttributes){
        ShoppingListItemEntity item = new ShoppingListItemEntity();
        item.setUserId(userId);

        Long ingredientId;
        IngredientEntity existingIngredient = ingredientRepository.findByUserIdAndName(userId, name);

        if(existingIngredient != null) {
            ingredientId = existingIngredient.getId();

            if(existingIngredient.isOnShoppingList()) {
                redirectAttributes.addFlashAttribute("error", "shoppingList.error.alreadyOnList");
                return;
            }
        } else {
            boolean success = ingredientListService.addIngredient(userId, name, redirectAttributes);

            if(!success) {
                return;
            }

            ingredientId = ingredientRepository.findByUserIdAndName(userId, name).getId();

            ingredientListService.toggleOnShoppingList(ingredientId);
        }
        item.setIngredientId(ingredientId);

        shoppingListItemRepository.save(item);
    }

    public void deleteItem(Long id) {
        ingredientListService.toggleOnShoppingList(id);
        shoppingListItemRepository.deleteById(id);
    }

    public List<ShoppingListData> getAllItems(Long userId) {
        List<ShoppingListData> items = shoppingListItemRepository.findShoppingListByUserId(userId);
        return items;
    }
}