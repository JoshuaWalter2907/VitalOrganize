package com.springboot.vitalorganize.repository;

import com.springboot.vitalorganize.model.IngredientEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IngredientRepository extends JpaRepository<IngredientEntity, Long> {

    @Query("SELECT i FROM IngredientEntity i WHERE i.name = :name AND i.userId = :userId")
    IngredientEntity findByUserIdAndName(
            @Param("userId") Long userId,
            @Param("name") String name);

    @Query("SELECT e FROM IngredientEntity e " +
            "WHERE e.userId = :userId AND e.id = :ingredientId")
    Optional<IngredientEntity> findByUserIdAndIngredientId(@Param("userId") Long userId,
                                                           @Param("ingredientId") Long ingredientId);

    @Modifying
    @Query("DELETE FROM IngredientEntity e " +
            "WHERE e.userId = :userId AND e.id = :ingredientId")
    void deleteByUserIdAndIngredientId(@Param("userId") Long userId,
                                       @Param("ingredientId") Long ingredientId);
    // Pagination methods
    @Query("SELECT i FROM IngredientEntity i WHERE i.userId = :userId AND i.favourite = true")
    Page<IngredientEntity> findByUserIdAndFavourite(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT i FROM IngredientEntity i WHERE i.userId = :userId AND i.onShoppingList = true")
    Page<IngredientEntity> findByUserIdAndOnShoppingList(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT i FROM IngredientEntity i WHERE i.userId = :userId")
    Page<IngredientEntity> findAllByUserId(@Param("userId") Long userId, Pageable pageable);
}
