package com.springboot.vitalorganize.service;

import com.springboot.vitalorganize.model.IngredientEntity;
import com.springboot.vitalorganize.model.IngredientRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Service
@AllArgsConstructor
public class IngredientListService {

    private final IngredientRepository ingredientRepository;
    private final SpoonacularService spoonacularService;
    private final TranslationService translationService;

    // Methode zum Hinzufügen einer Zutat
    // limited to 100 api calls per day
    public boolean addIngredient(
            Long userId,
            String name,
            RedirectAttributes redirectAttributes){

        String englishName = translationService.translateQuery(name, "de", "en");
        if(englishName.startsWith("Translation failed:")){
            // on translation fail (500 character api limit reached), try it with the original name, might be in english already
            englishName = name;
        }

        if(ingredientRepository.findByUserIdAndName(userId, name) != null ||
        ingredientRepository.findByUserIdAndName(userId, englishName) != null){
            redirectAttributes.addFlashAttribute("error", "ingredient.error.alreadyOnList");
            return false;
        }

        Map<String, Object> ingredientData;
        try {
            ingredientData = spoonacularService.getIngredientData(englishName);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", "ingredient.error.notFound");
            return false;
        }

        IngredientEntity ingredient = new IngredientEntity();
        ingredient.setUserId(userId);
        // turns name uppercase
        ingredient.setName(englishName.substring(0, 1).toUpperCase() + englishName.substring(1));

        // set other stuff taken from api
        ingredient.setCategory((String) ingredientData.get("category"));
        ingredient.setAmount((double) ingredientData.get("amount"));
        ingredient.setUnit((String) ingredientData.get("unit"));
        ingredient.setPrice((double) ingredientData.get("estimatedCostInEuros"));

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
