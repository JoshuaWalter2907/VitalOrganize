package com.springboot.vitalorganize.repository;

import com.springboot.vitalorganize.model.ShoppingListData;
import com.springboot.vitalorganize.entity.ShoppingListItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShoppingListItemRepository extends JpaRepository<ShoppingListItemEntity, Long> {

    boolean existsByUserIdAndIngredientId(Long userId, Long itemId);

    Optional<ShoppingListItemEntity> findByUserIdAndIngredientId(Long userId, Long ingredientId);

    @Modifying
    void deleteByUserIdAndIngredientId(Long userId, Long ingredientId);


    // joins ingredients and shoppingList table to fetch all the information for the full shopping list
    @Query("SELECT new com.springboot.vitalorganize.model.ShoppingListData(" +
            "i.id, i.name, s.purchaseAmount, i.price, s.calculatedPrice)" +
            "FROM ShoppingListItemEntity s " +
            "RIGHT JOIN IngredientEntity i ON s.userId = i.userId AND s.ingredientId = i.id " +
            "WHERE i.userId = :userId AND i.onShoppingList = true")
    List<ShoppingListData> findShoppingListByUserId(@Param("userId") Long userId);

    // joins ingredients and shoppingList table to fetch all the information for a single shopping list item
    @Query("SELECT new com.springboot.vitalorganize.model.ShoppingListData(" +
            "i.id, i.name, s.purchaseAmount, i.price, s.calculatedPrice) " +
            "FROM ShoppingListItemEntity s " +
            "RIGHT JOIN IngredientEntity i ON s.userId = i.userId AND s.ingredientId = i.id " +
            "WHERE i.userId = :userId AND i.id = :ingredientId AND i.onShoppingList = true")
    Optional<ShoppingListData> findShoppingListItemByUserIdAndIngredientId(@Param("userId") Long userId,
                                                                           @Param("ingredientId") Long ingredientId);
}
