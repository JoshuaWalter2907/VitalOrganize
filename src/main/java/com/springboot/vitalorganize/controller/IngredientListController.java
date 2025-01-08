package com.springboot.vitalorganize.controller;

import com.springboot.vitalorganize.model.IngredientEntity;
import com.springboot.vitalorganize.repository.IngredientRepository;
import com.springboot.vitalorganize.service.IngredientListService;
import com.springboot.vitalorganize.service.ShoppingListService;
import com.springboot.vitalorganize.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@AllArgsConstructor
@RequestMapping("/ingredients")
public class IngredientListController {

    private final IngredientRepository ingredientRepository;
    private final IngredientListService ingredientService;
    private final UserService userService;
    private final ShoppingListService shoppingListService;

    // loads the main ingredients page
    @GetMapping
    public String listIngredients(Model model,
                                  @AuthenticationPrincipal OAuth2User user,
                                  OAuth2AuthenticationToken token,
                                  HttpServletRequest request,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(required=false) String sort,
                                  @RequestParam(required=false) String filter
    ) {
        int pageSize = 8;
        Long userId = userService.getCurrentUser(user, token).getId();
        HttpSession session = request.getSession();

        // check for current filter
        if (filter != null && filter.equals(session.getAttribute("filter"))) {      // same filter selected again removes the filter
            filter = "";
            session.setAttribute("filter", filter);
            session.setAttribute("page", 0);
        } else if (filter != null) {                                                   // new filter is selected
            session.setAttribute("filter", filter);
            session.setAttribute("page", 0);
        } else {                                                                       // page loaded for the first time or was reloaded
            filter = (String) session.getAttribute("filter");
            if(filter == null) {
                filter = "";
            }
        }

        // check for current sorting
        if (sort == null) {
            sort = (String) session.getAttribute("sort");
            if(sort == null) {
                sort = "insertionDate";
            }
        } else {
            session.setAttribute("sort", sort);
        }

        // check if page was set to default or passed as a parameter
        String pageParam = request.getParameter("page");
        if (pageParam != null && !pageParam.isEmpty()) {
            page = Integer.parseInt(pageParam);
            session.setAttribute("page", page);
        } else {
            Integer sessionPage = (Integer) session.getAttribute("page");
            if (sessionPage != null) {
                page = sessionPage;
            }
            session.setAttribute("page", page);
        }

        Pageable pageable = PageRequest.of(page, pageSize);

        // get paginated, sorted and filtered ingredients
        Page<IngredientEntity> ingredientsPage = ingredientService.getAllIngredients(userId, pageable, sort, filter);

        if(ingredientsPage.isEmpty()) {
            // redirect to first page if user manipulated the link
            pageable = PageRequest.of(0, pageSize);
            ingredientsPage = ingredientService.getAllIngredients(userId, pageable, sort, filter);
        }

        model.addAttribute("ingredients", ingredientsPage.getContent());
        model.addAttribute("filter", filter);
        model.addAttribute("sort", sort);
        model.addAttribute("page", ingredientsPage);

        return "ingredientsList/ingredients";
    }

    // add ingredient
    @PostMapping("/add")
    public String addIngredient(
            @RequestParam(value = "newIngredient") String name,
            RedirectAttributes redirectAttributes,
            @AuthenticationPrincipal OAuth2User user,
            OAuth2AuthenticationToken token) {
        Long userId = userService.getCurrentUser(user, token).getId();

        ingredientService.addIngredient(userId, name, redirectAttributes);

        return "redirect:/ingredients";
    }

    // delete ingredient
    @PostMapping("/delete/{id}")
    public String deleteIngredient(@PathVariable("id") Long id,
                                   @AuthenticationPrincipal OAuth2User user,
                                   OAuth2AuthenticationToken token) {
        Long userId = userService.getCurrentUser(user, token).getId();

        ingredientService.deleteIngredient(userId, id);
        return "redirect:/ingredients";
    }

    // toggles the favourite status
    @PostMapping("/favourite/{id}")
    public String toggleFavouriteIngredient(@PathVariable("id") Long id,
                                            @AuthenticationPrincipal OAuth2User user,
                                            OAuth2AuthenticationToken token) {
        Long userId = userService.getCurrentUser(user, token).getId();

        ingredientService.toggleFavourite(userId, id);
        return "redirect:/ingredients";
    }

    // toggles the onShoppingList status
    @PostMapping("/onShoppingList/{id}")
    public String toggleOnShoppingList(@PathVariable("id") Long id,
                                       RedirectAttributes redirectAttributes,
                                       @AuthenticationPrincipal OAuth2User user,
                                       OAuth2AuthenticationToken token) {
        Long userId = userService.getCurrentUser(user, token).getId();

        IngredientEntity ingredient = ingredientRepository.findByUserIdAndIngredientId(userId, id).orElseThrow();

        if(!ingredient.isOnShoppingList()){
            Long user_id  = userService.getCurrentUser(user, token).getId();
            shoppingListService.addItem(user_id, ingredient.getName(), redirectAttributes);
        } else {
            shoppingListService.deleteItem(userId, id);
        }

        return "redirect:/ingredients";
    }


}