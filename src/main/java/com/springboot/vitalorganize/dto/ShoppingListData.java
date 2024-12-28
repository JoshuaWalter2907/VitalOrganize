package com.springboot.vitalorganize.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ShoppingListData {
    private Long ingredientId;
    private String name;
    private double purchaseAmount;
    private double price;
    private double amount;
    private String unit;
    private double calculatedPrice;

    // Getter und Setter


    public ShoppingListData(Long ingredientId,
                            String name,
                            double purchaseAmount,
                            double price,
                            double amount,
                            String unit,
                            double calculatedPrice) {
        this.ingredientId = ingredientId;
        this.name = name;
        this.purchaseAmount = purchaseAmount;
        this.price = price;
        this.amount = amount;
        this.unit = unit;
        this.calculatedPrice = calculatedPrice;
    }

}
