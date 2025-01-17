package com.springboot.vitalorganize.model;

import java.time.LocalDate;

public class Meal {

    private int id;
    private int userId;
    private int recipeId;
    private LocalDate cookDate;

    // Getter und Setter
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(int recipeId) {
        this.recipeId = recipeId;
    }

    public LocalDate getCookDate() {
        return cookDate;
    }

    public void setCookDate(LocalDate cookDate) {
        this.cookDate = cookDate;
    }
}
