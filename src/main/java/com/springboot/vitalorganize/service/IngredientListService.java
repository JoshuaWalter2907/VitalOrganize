package com.springboot.vitalorganize.service;

import com.springboot.vitalorganize.model.IngredientEntity;
import com.springboot.vitalorganize.model.IngredientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;

@Service
public class IngredientListService {

    private final IngredientRepository ingredientRepository;

    @Autowired
    public IngredientListService(IngredientRepository ingredientRepository) {
        this.ingredientRepository = ingredientRepository;
    }

    // Methode zum Hinzufügen einer Zutat
    public boolean addIngredient(
            Long userId,
            String name,
            RedirectAttributes redirectAttributes){

        if(ingredientRepository.findByUserIdAndName(userId, name) != null){
            redirectAttributes.addFlashAttribute("error", "ingredient.error.alreadyOnList");
            return false;
        }

        /*
        //TODO LATER: API CALL TO GET THE REST OF THE INFO
        boolean apiResultFound = false;
        if(!apiResultFound) {
            redirectAttributes.addFlashAttribute("error", "ingredient.error.notFound");
            return false;
        }*/

        IngredientEntity ingredient = new IngredientEntity();
        ingredient.setUserId(userId);
        ingredient.setName(name);
        // set other stuff taken from api

        // save the ingredient
        ingredientRepository.save(ingredient);
        return true;
    }

    public void deleteIngredient(Long id) {
        ingredientRepository.deleteById(id);
    }

    public void toggleFavourite(Long id) {
        IngredientEntity ingredient = ingredientRepository.findById(id).orElseThrow(() -> new RuntimeException("Ingredient not found"));
        ingredient.setFavourite(!ingredient.isFavourite()); // invert the favourite status
        ingredientRepository.save(ingredient);
    }

    public void toggleOnShoppingList(Long id) {
        IngredientEntity ingredient = ingredientRepository.findById(id).orElseThrow(() -> new RuntimeException("Ingredient not found"));
        ingredient.setOnShoppingList(!ingredient.isOnShoppingList()); // invert the onShoppingList status
        ingredientRepository.save(ingredient);
    }

    public List<IngredientEntity> getAllIngredients(Long userId) {
        List<IngredientEntity> ingredients = ingredientRepository.findAllByUserId(userId);
        return ingredients;
    }
}
