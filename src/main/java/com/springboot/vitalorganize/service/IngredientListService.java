package com.springboot.vitalorganize.service;

import com.springboot.vitalorganize.entity.IngredientEntity;
import com.springboot.vitalorganize.entity.UserEntity;
import com.springboot.vitalorganize.repository.IngredientRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Service
@AllArgsConstructor
public class IngredientListService {

    private final IngredientRepository ingredientRepository;
    private final SpoonacularService spoonacularService;
    private final TranslationService translationService;
    private final SenderService senderService;
    private final UserService userService;

    // Methode zum Hinzufügen einer Zutat
    // limited to 100 api calls per day
    public boolean addIngredient(
            Long userId,
            String name,
            RedirectAttributes redirectAttributes){

        String englishName = translationService.translateQuery(name, "de", "en");
        if(englishName.startsWith("Translation failed:")){
            // on translation fail try it with the original name, might be in english already
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
        ingredient.setPrice((double) ingredientData.get("estimatedCostInEuros"));

        // save the ingredient
        ingredientRepository.save(ingredient);
        return true;
    }

    @Transactional
    public void deleteIngredient(Long userId, Long ingredientId) {
        ingredientRepository.deleteByUserIdAndIngredientId(userId, ingredientId);
    }

    public void toggleFavourite(Long userId, Long ingredientId) {
        IngredientEntity ingredient = ingredientRepository.findByUserIdAndIngredientId(userId, ingredientId).orElseThrow(() -> new RuntimeException("Ingredient not found"));
        ingredient.setFavourite(!ingredient.isFavourite()); // invert the favourite status
        ingredientRepository.save(ingredient);
    }

    public void toggleOnShoppingList(Long userId, Long ingredientId) {
        IngredientEntity ingredient = ingredientRepository.findByUserIdAndIngredientId(userId, ingredientId).orElseThrow(() -> new RuntimeException("Ingredient not found"));
        ingredient.setOnShoppingList(!ingredient.isOnShoppingList()); // invert the onShoppingList status
        ingredientRepository.save(ingredient);
    }

    public Page<IngredientEntity> getAllIngredients(Long userId, Pageable pageable, String sort, String filter) {
        // determine sorting functionality, default is sorting by ascending insertion date
        Sort.Direction direction = Sort.Direction.ASC;
        String sortBy = "id";

        switch (sort) {
            case "insertionDate":
                sortBy = "id";
                break;
            case "insertionDateReverse":
                direction = Sort.Direction.DESC;
                sortBy = "id";
                break;
            case "alphabetical":
                sortBy = "name";
                break;
            case "alphabeticalReverse":
                direction = Sort.Direction.DESC;
                sortBy = "name";
                break;
            default:
                break;
        }

        Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(direction, sortBy));

        if (filter.equals("favourite")) {
            return ingredientRepository.findByUserIdAndFavourite(userId, sortedPageable);
        } else if (filter.equals("shoppingList")) {
            return ingredientRepository.findByUserIdAndOnShoppingList(userId, sortedPageable);
        }

        // without filters
        return ingredientRepository.findAllByUserId(userId, sortedPageable);
    }

    // send an email with all the prices of your favorite ingredients
    public void sendEmailWithPrices(Long userId){
        Pageable pageable = PageRequest.of(0, 1);
        Page<IngredientEntity> favoriteIngredients = ingredientRepository.findByUserIdAndFavourite(userId, pageable);
        StringBuilder emailText = new StringBuilder("Dear Customer,\n\nthese are the current prices for all your favourite ingredients: \n\n------------------------------\n");

        for (int i=0; i<favoriteIngredients.getTotalPages(); i++) {
            favoriteIngredients = ingredientRepository.findByUserIdAndFavourite(userId, pageable.withPage(i));

            for (IngredientEntity ingredient : favoriteIngredients.getContent()) {
                emailText.append(String.format("%s: %.2f€/100g\n", ingredient.getName(), ingredient.getPrice()));
            }
        }
        emailText.append("------------------------------\n\nKind regards,\nYour VitalOrganize Team");
        // Email abrufen
        UserEntity userEntity = userService.getUserById(userId);
        String email = userEntity.getEmail();
        // Alternative E-Mail für bestimmte Provider verwenden
        if ("github".equals(userEntity.getProvider()) && userEntity.getSendtoEmail() != null) {
            email = userEntity.getSendtoEmail();
        }

        senderService.sendEmail(email, "Report of your favourite ingredients and their prices", emailText.toString());
    }
}