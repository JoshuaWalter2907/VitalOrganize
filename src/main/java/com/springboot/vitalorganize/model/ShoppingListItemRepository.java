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
            "i.id, i.name, s.purchaseAmount, i.price, i.amount, i.unit, (s.purchaseAmount * i.price)) " +
            "FROM ShoppingListItemEntity s " +
            "INNER JOIN IngredientEntity i ON s.userId = i.userId AND s.ingredientId = i.id " +
            "WHERE s.userId = :userId")
    List<ShoppingListData> findShoppingListByUserId(
            @Param("userId") Long userId);
}
