package com.springboot.vitalorganize.model.Shopping_List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
public class ShoppingListData {
    private Long ingredientId;
    private String name;
    private double purchaseAmountInGrams;
    private double priceInEuroPerHundredGrams;
    private double calculatedPriceInEuros;
}
