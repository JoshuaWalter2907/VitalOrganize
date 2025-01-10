package com.springboot.vitalorganize.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "shopping_list")
public class ShoppingListItemEntity {

    @Id
    @Column(name = "ingredient_id", nullable = false)
    private Long ingredientId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "purchase_amount")
    private double purchaseAmount = 0;

    @Column(name = "calculated_price")
    private double calculatedPrice = 0;

    // foreign key user_id on id in users table
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", insertable = false, updatable = false)
    private UserEntity user;

    // foreign key ingredient_id on id in ingredients table
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ingredient_id", referencedColumnName = "id", insertable = false, updatable = false)
    private IngredientEntity ingredient;
}
