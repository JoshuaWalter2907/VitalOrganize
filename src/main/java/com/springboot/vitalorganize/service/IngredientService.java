package com.springboot.vitalorganize.service;

import com.springboot.vitalorganize.model.IngredientEntity;
import com.springboot.vitalorganize.model.IngredientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IngredientService {

    private final IngredientRepository ingredientRepository;

    @Autowired
    public IngredientService(IngredientRepository ingredientRepository) {
        this.ingredientRepository = ingredientRepository;
    }

    // Methode zum Hinzufügen einer Zutat
    public void addIngredient(String name){
        IngredientEntity ingredient = new IngredientEntity();
        ingredient.setName(name);
        //TODO LATER: API CALL TO GET THE REST OF THE INFO
        ingredientRepository.save(ingredient); // this adds a new row in the ingredients table
    }

    public void deleteIngredient(Long id) {
        ingredientRepository.deleteById(id);
    }

    public void toggleFavourite(Long id) {
        IngredientEntity ingredient = ingredientRepository.findById(id).orElseThrow(() -> new RuntimeException("Ingredient not found"));
        ingredient.setFavourite(!ingredient.isFavourite()); // invert the favourite status
        ingredientRepository.save(ingredient);
    }

    public List<IngredientEntity> getAllIngredients() {
        List<IngredientEntity> ingredients = ingredientRepository.findAll();
        System.out.println("Service: " + ingredients);
        return ingredients;
    }
}
