package com.springboot.vitalorganize.model;

import com.springboot.vitalorganize.dto.ShoppingListData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShoppingListItemRepository extends JpaRepository<ShoppingListItemEntity, Long> {

    @Query("SELECT new com.springboot.vitalorganize.dto.ShoppingListData(" +
            "i.id, i.name, COALESCE(s.purchaseAmount, 0), COALESCE(i.price, 0), i.amount, i.unit, COALESCE(s.calculatedPrice, 0))" +
            "FROM ShoppingListItemEntity s " +
            "RIGHT JOIN IngredientEntity i ON s.userId = i.userId AND s.ingredientId = i.id " +
            "WHERE i.userId = :userId AND i.onShoppingList = true")
    List<ShoppingListData> findShoppingListByUserId(@Param("userId") Long userId);
}
