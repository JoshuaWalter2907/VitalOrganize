package com.springboot.vitalorganize.model;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "ingredients")
public class IngredientEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    private double price;

    private double amount;

    private String unit;

    private String category;

    @Column(nullable = false)
    private boolean favourite = false;

    @Column(nullable = false)
    private boolean isOnShoppingList = false;
}
