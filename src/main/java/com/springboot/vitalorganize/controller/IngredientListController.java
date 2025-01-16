package com.springboot.vitalorganize.controller;

import com.springboot.vitalorganize.entity.IngredientEntity;
import com.springboot.vitalorganize.entity.Profile_User.UserEntity;
import com.springboot.vitalorganize.model.Ingredient.IngredientListData;
import com.springboot.vitalorganize.repository.IngredientRepository;
import com.springboot.vitalorganize.service.IngredientListService;
import com.springboot.vitalorganize.service.ShoppingListService;
import com.springboot.vitalorganize.service.TranslationService;
import com.springboot.vitalorganize.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@AllArgsConstructor
@RequestMapping("/ingredients")
public class IngredientListController {

    private final IngredientRepository ingredientRepository;
    private final IngredientListService ingredientListService;
    private final UserService userService;
    private final ShoppingListService shoppingListService;
    private final TranslationService translationService;

    // load the ingredients page
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

    // add the ingredient
    @PostMapping("/add")
    public String addIngredient(@RequestParam(value = "newIngredient") String name,
                                RedirectAttributes attr) {
        try{
            translationService.translateQuery(name, "de", "en");
            ingredientListService.addIngredient(name);
        } catch (IllegalArgumentException e){
            attr.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/ingredients";
    }

    // delete the ingredient
    @PostMapping("/delete/{id}")
    public String deleteIngredient(@PathVariable("id") Long id) {
        ingredientListService.deleteIngredient(id);
        return "redirect:/ingredients";
    }

    // toggle the favourite status
    @PostMapping("/favourite/{id}")
    public String toggleFavouriteIngredient(@PathVariable("id") Long id) {
        ingredientListService.toggleFavourite(id);
        return "redirect:/ingredients";
    }

    // toggle the onShoppingList status
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

    // toggle the priceReportEnabled status
    @PostMapping("/priceReportEmail")
    public String togglePriceReportEmail() {
        userService.togglePriceReportEmail();
        return "redirect:/ingredients";
    }

    // send the price report via email
    @PostMapping("/sendPriceReportEmail")
    public String sendPriceReportEmail() {
        UserEntity userEntity = userService.getCurrentUser();
        Long userId = userEntity.getId();

        // premium function, requires membership
        if(userEntity.isMember()){
            ingredientListService.sendEmailWithPrices(userId);
        }
        return "redirect:/ingredients";
    }
}