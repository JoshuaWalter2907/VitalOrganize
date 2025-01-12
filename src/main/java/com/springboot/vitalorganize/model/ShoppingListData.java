package com.springboot.vitalorganize.model;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ShoppingListData {
    private Long ingredientId;
    private String name;
    private double purchaseAmountInGrams;
    private double priceInEuroPerHundredGrams;
    private double calculatedPriceInEuros;

    // Getter und Setter

    public ShoppingListData(Long ingredientId,
                            String name,
                            double purchaseAmount,
                            double price,
                            double calculatedPrice) {
        this.ingredientId = ingredientId;
        this.name = name;
        this.purchaseAmountInGrams = purchaseAmount;
        this.priceInEuroPerHundredGrams = price;
        this.calculatedPriceInEuros = calculatedPrice;
    }

}
