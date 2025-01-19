package com.springboot.vitalorganize.model;

import java.time.LocalDate;

public class Meal {

    private Long id;
    private Long userId;
    private Long recipeId;
    private LocalDate cookDate;

    // Getter und Setter
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(Long recipeId) {
        this.recipeId = recipeId;
    }

    public LocalDate getCookDate() {
        return cookDate;
    }

    public void setCookDate(LocalDate cookDate) {
        this.cookDate = cookDate;
    }
}
