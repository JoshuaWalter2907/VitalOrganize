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

    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN TRUE ELSE FALSE END " +
            "FROM IngredientEntity e " +
            "WHERE e.userId = :userId AND e.id = :itemId")
    boolean existsByUserIdAndItemId(@Param("userId") Long userId,
                                    @Param("itemId") Long itemId);

    @Query("SELECT s FROM ShoppingListItemEntity s " +
            "WHERE s.userId = :userId AND s.ingredientId = :ingredientId")
    Optional<ShoppingListItemEntity> findByUserIdAndIngredientId(@Param("userId") Long userId,
                                                       @Param("ingredientId") Long ingredientId);

    @Modifying
    @Query("DELETE FROM ShoppingListItemEntity s " +
            "WHERE s.userId = :userId AND s.ingredientId = :itemId")
    void deleteByUserIdAndItemId(@Param("userId") Long userId,
                                 @Param("itemId") Long itemId);
    @Query("SELECT new com.springboot.vitalorganize.model.ShoppingListData(" +
            "i.id, i.name, s.purchaseAmount, i.price, s.calculatedPrice)" +
            "FROM ShoppingListItemEntity s " +
            "RIGHT JOIN IngredientEntity i ON s.userId = i.userId AND s.ingredientId = i.id " +
            "WHERE i.userId = :userId AND i.onShoppingList = true")
    List<ShoppingListData> findShoppingListByUserId(@Param("userId") Long userId);


    @Query("SELECT new com.springboot.vitalorganize.model.ShoppingListData(" +
            "i.id, i.name, s.purchaseAmount, i.price, s.calculatedPrice) " +
            "FROM ShoppingListItemEntity s " +
            "RIGHT JOIN IngredientEntity i ON s.userId = i.userId AND s.ingredientId = i.id " +
            "WHERE i.userId = :userId AND i.id = :ingredientId AND i.onShoppingList = true")
    Optional<ShoppingListData> findShoppingListItemByUserIdAndIngredientId(@Param("userId") Long userId,
                                                                           @Param("ingredientId") Long ingredientId);
}
