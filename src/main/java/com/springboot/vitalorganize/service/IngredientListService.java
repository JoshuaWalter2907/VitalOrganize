package com.springboot.vitalorganize.service;

import com.springboot.vitalorganize.entity.IngredientEntity;
import com.springboot.vitalorganize.entity.Profile_User.UserEntity;
import com.springboot.vitalorganize.model.IngredientListData;
import com.springboot.vitalorganize.repository.IngredientRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Dieser Service ist zuständig für die Funktionalität der Zutatenseite
 * Diese Klasse bietet Methoden für die Zutatenliste und zum Senden einer
 */
@Service
@AllArgsConstructor
public class IngredientListService {

    private final IngredientRepository ingredientRepository;
    private final SpoonacularService spoonacularService;
    private final UserService userService;


    /**
     * Fügt der Zutatenliste eine Zutat hinzu
     * <p>
     * Nur 100 api calls möglich pro Tag, 2 calls nötig pro neuer Zutat
     *
     * @param userId Die UserId des Nutzers
     * @param name Der Name der Zutat
     */
    public void addIngredient(Long userId, String name){
        if(ingredientRepository.findByUserIdAndName(userId, name).orElse(null) != null){
            throw new IllegalArgumentException("ingredient.error.alreadyOnList");
        }

        Map<String, Object> ingredientData;
        try {
            ingredientData = spoonacularService.getIngredientData(name);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("ingredient.error.notFound");
        }

        IngredientEntity ingredient = new IngredientEntity();
        ingredient.setUserId(userId);

        // turns ingredient name uppercase
        ingredient.setName(name.substring(0, 1).toUpperCase() + name.substring(1));

        // set ingredient info from api
        ingredient.setCategory((String) ingredientData.get("category"));
        ingredient.setPrice((double) ingredientData.get("estimatedCostInEuros"));

        ingredientRepository.save(ingredient);
    }

    /**
     * Löscht eine Zutat von der Zutatenliste
     * @param ingredientId Die Id der Zutat
     */
    @Transactional
    public void deleteIngredient(Long ingredientId) {
        Long userId = userService.getCurrentUser().getId();

        ingredientRepository.deleteByUserIdAndId(userId, ingredientId);
    }

    /**
     * Verändert den "favourite"-Status einer Zutat
     * @param ingredientId Die Id der Zutat
     */
    public void toggleFavourite(Long ingredientId) {
        Long userId = userService.getCurrentUser().getId();
        IngredientEntity ingredient = ingredientRepository.findByUserIdAndId(userId, ingredientId).orElseThrow();

        ingredient.setFavourite(!ingredient.isFavourite()); // inverts the favourite status
        ingredientRepository.save(ingredient);
    }

    /**
     * Verändert den "onShoppingList"-Status einer Zutat
     * @param userId Die UserId des Nutzers
     * @param ingredientId Die Id der Zutat
     */
    public void toggleOnShoppingList(Long userId, Long ingredientId) {
        IngredientEntity ingredient = ingredientRepository.findByUserIdAndId(userId, ingredientId).orElseThrow();

        ingredient.setOnShoppingList(!ingredient.isOnShoppingList()); // inverts the onShoppingList status
        ingredientRepository.save(ingredient);
    }

    /**
     * Gibt Zutatendaten zurück
     * @param request Wird zum Zugriff auf die Session des aktuellen Nutzers verwendet
     * @param page Die derzeitige Page der Zutatenliste
     * @param sort Die derzeitige Sortierung der Zutatenliste
     * @param filter Der derzeitige Filter der Zutatenliste
     * @return Zutatendaten
     */
    public IngredientListData getAllIngredients(HttpServletRequest request, int page, String sort, String filter) {
        int pageSize = 10;

        UserEntity userEntity = userService.getCurrentUser();
        Long userId = userEntity.getId();
        HttpSession session = request.getSession();

        // check current filter
        if (filter != null && filter.equals(session.getAttribute("filter"))) {
            // same filter selected again removes the filter
            filter = "";
            session.setAttribute("filter", filter);
            session.setAttribute("page", 0);
        } else if (filter != null) {
            // new filter is selected
            session.setAttribute("filter", filter);
            session.setAttribute("page", 0);
        } else {
            // page loaded for the first time -> no filter, page was reloaded -> filter from session
            filter = (String) session.getAttribute("filter");
            if (filter == null) {
                filter = "";
            }
        }

        // check current sorting
        if (sort == null) {
            sort = (String) session.getAttribute("sort");
            if (sort == null) {
                // default sorting
                sort = "insertionDateReverse";
            }
        } else {
            session.setAttribute("sort", sort);
        }

        // check whether page was passed as parameter (if not, use page from session or default value)
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
        Page<IngredientEntity> ingredientsPage = getPaginatedIngredients(userId, pageable, sort, filter);

        if (ingredientsPage.isEmpty()) {
            // redirect to first page if user manipulated the link
            pageable = PageRequest.of(0, pageSize);
            ingredientsPage = getPaginatedIngredients(userId, pageable, sort, filter);
        }

        return new IngredientListData(ingredientsPage, filter, sort);
    }

    /**
     * Gibt Eine Page mit Zutatendaten zurück
     * @param userId Die UserId des Nutzers
     * @param pageable Pageable
     * @param sort Die derzeitige Sortierung der Zutatenliste
     * @param filter Der derzeitige Filter der Zutatenliste
     * @return Page mit Zutatendaten
     */
    public Page<IngredientEntity> getPaginatedIngredients(Long userId, Pageable pageable, String sort, String filter){
        // determine sorting method, default is by descending insertion date (newest first)
        Sort.Direction direction = Sort.Direction.DESC;
        String sortBy = "id";

        switch (sort) {
            case "insertionDate":
                direction = Sort.Direction.ASC;
                sortBy = "id";
                break;
            case "insertionDateReverse":
                sortBy = "id";
                break;
            case "alphabetical":
                direction = Sort.Direction.ASC;
                sortBy = "name";
                break;
            case "alphabeticalReverse":
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
}