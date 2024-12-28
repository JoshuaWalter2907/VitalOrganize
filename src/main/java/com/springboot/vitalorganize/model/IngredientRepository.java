package com.springboot.vitalorganize.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IngredientRepository extends JpaRepository<IngredientEntity, Long> {

    @Query("SELECT i FROM IngredientEntity i WHERE i.userId = :userId")
    List<IngredientEntity> findAllByUserId(
            @Param("userId") Long userId);

    @Query("SELECT i FROM IngredientEntity i WHERE i.name = :name AND i.userId = :userId")
    IngredientEntity findByUserIdAndName(
            @Param("userId") Long userId,
            @Param("name") String name);
}
