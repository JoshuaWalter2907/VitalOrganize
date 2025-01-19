package com.springboot.vitalorganize.service;

import com.springboot.vitalorganize.entity.IngredientEntity;
import com.springboot.vitalorganize.entity.Profile_User.UserEntity;
import com.springboot.vitalorganize.repository.IngredientRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * Dieser Service ist zuständig für das Senden einer Email mit Preisen zu allen Lieblingszutaten eines Nutzers
 * Diese Klasse bietet eine Methode für das Senden der Email
 */
@Service
@AllArgsConstructor
public class PriceReportEmailService {
    private final IngredientRepository ingredientRepository;
    private final UserService userService;
    private final SenderService senderService;

    /**
     * Sendet eine Email mit den Preisen aller Lieblingszutaten des Nutzers
     * @param userId Die UserId des Nutzers
     */
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

        UserEntity userEntity = userService.getUserById(userId);
        String email = userEntity.getEmail();

        // use alternative email for provider github
        if ("github".equals(userEntity.getProvider()) && userEntity.getSendToEmail() != null) {
            email = userEntity.getSendToEmail();
        }

        senderService.sendEmail(email, "Report of your favourite ingredients and their prices", emailText.toString());
    }
}
