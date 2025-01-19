package com.springboot.vitalorganize.controller;

import com.springboot.vitalorganize.entity.IngredientEntity;
import com.springboot.vitalorganize.entity.Profile_User.UserEntity;
import com.springboot.vitalorganize.model.IngredientListData;
import com.springboot.vitalorganize.repository.IngredientRepository;
import com.springboot.vitalorganize.service.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


/**
 * Controller für die Ingredient-Page
 */
@Controller
@AllArgsConstructor
@RequestMapping("/ingredients")
public class IngredientListController {

    private final IngredientRepository ingredientRepository;
    private final IngredientListService ingredientListService;
    private final UserService userService;
    private final ShoppingListService shoppingListService;
    private final TranslationService translationService;
    private final PriceReportEmailService priceReportEmailService;

    /**
     * Zeigt die Zutatenliste des Nutzers an
     * @param model Das Model für die View
     * @param request Wird zum Zugriff auf die Session des aktuellen Nutzers verwendet
     * @param page Die derzeitige Page der Zutatenliste
     * @param sort Die derzeitige Sortierung der Zutatenliste
     * @param filter Der derzeitige Filter der Zutatenliste
     * @return Ingredient-Page
     */
    @GetMapping
    public String listIngredients(Model model,
                                  HttpServletRequest request,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(required=false) String sort,
                                  @RequestParam(required=false) String filter
    ) {
        UserEntity userEntity = userService.getCurrentUser();

        IngredientListData ingredientListData = ingredientListService.getAllIngredients(request, page, sort, filter);

        model.addAttribute("page", ingredientListData.getPage());
        model.addAttribute("ingredients", ingredientListData.getPage().getContent());   // returns a list of ingredientEntities
        model.addAttribute("filter", ingredientListData.getFilter());
        model.addAttribute("sort", ingredientListData.getSort());
        model.addAttribute("priceReportsEnabled", userEntity.isPriceReportsEnabled());

        return "ingredientsList/ingredients";
    }

    /**
     * Fügt der Zutatenliste eine Zutat hinzu
     * @param name Der Name der Zutat
     * @param attr Wird verwendet, um Fehlermeldungen an die View zu übermitteln
     * @return Ingredient-Page
     */
    @PostMapping("/add")
    public String addIngredient(@RequestParam(value = "newIngredient") String name,
                                RedirectAttributes attr) {
        Long userId = userService.getCurrentUser().getId();
        try{
            name = translationService.translateQuery(name, "de", "en");
            ingredientListService.addIngredient(userId, name);
        } catch (IllegalArgumentException e){
            attr.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/ingredients";
    }

    /**
     * Löscht eine Zutat von der Zutatenliste
     * @param id Die Id der Zutat
     * @return Ingredient-Page
     */
    @PostMapping("/delete/{id}")
    public String deleteIngredient(@PathVariable("id") Long id) {
        ingredientListService.deleteIngredient(id);
        return "redirect:/ingredients";
    }

    /**
     * Verändert den "favourite"-Status einer Zutat
     * @param id Die Id der Zutat
     * @return Ingredient-Page
     */
    @PostMapping("/favourite/{id}")
    public String toggleFavouriteIngredient(@PathVariable("id") Long id) {
        ingredientListService.toggleFavourite(id);
        return "redirect:/ingredients";
    }

    /**
     * Verändert den "onShoppingList"-Status einer Zutat
     * @param id Die Id der Zutat
     * @param attr Wird verwendet, um Fehlermeldungen an die View zu übermitteln
     * @return Ingredient-Page
     */
    @PostMapping("/onShoppingList/{id}")
    public String toggleOnShoppingList(@PathVariable("id") Long id,
                                       RedirectAttributes attr) {
        Long userId = userService.getCurrentUser().getId();
        IngredientEntity ingredient = ingredientRepository.findByUserIdAndId(userId, id).orElseThrow();

        try{
            if(!ingredient.isOnShoppingList()){
                shoppingListService.addItem(userId, ingredient.getName());
            } else {
                shoppingListService.deleteItem(userId, id);
            }
        } catch (IllegalArgumentException e) {
            attr.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/ingredients";
    }

    /**
     * Verändert den "priceReportsEnabled"-Status eines Nutzers
     * @return Ingredient-Page
     */
    @PostMapping("/priceReportEmail")
    public String togglePriceReportEmail() {
        userService.togglePriceReportEmail();
        return "redirect:/ingredients";
    }

    /**
     * Schickt dem Nutzer eine Email mit den Preisen seiner Lieblingszutaten
     * @return Ingredient-Page
     */
    @PostMapping("/sendPriceReportEmail")
    public String sendPriceReportEmail() {
        UserEntity userEntity = userService.getCurrentUser();
        Long userId = userEntity.getId();

        // premium function, requires membership
        if(userEntity.isMember()){
            priceReportEmailService.sendEmailWithPrices(userId);
        }
        return "redirect:/ingredients";
    }
}