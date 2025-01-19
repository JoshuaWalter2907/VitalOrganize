package com.springboot.vitalorganize.model;

import java.util.List;
import java.util.Map;

public class Recipe {
    private int id;
    private List<String> diet;
    private String difficulty;
    private List<String> image_urls;
    private List<Ingredient> ingredients;
    private String keywords;
    private Nutrition nutrition;
    private int portions;
    private Rating rating;
    private String source;
    private String source_url;
    private String title;
    private double totalTime;
    private Long user_id;

    public String getId() {
        return "" + id;
    }

    public void setId(String id) {
        this.id = Integer.parseInt(id);
    }

    public List<String> getDiet() {
        return diet;
    }

    public void setDiet(List<String> diet) {
        this.diet = diet;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public List<String> getImage_urls() {
        return image_urls;
    }

    public void setImage_urls(List<String> image_urls) {
        this.image_urls = image_urls;
    }

    public List<Ingredient> getIngredients() {
        return ingredients;
    }

    public void setIngredients(List<Ingredient> ingredients) {
        this.ingredients = ingredients;
    }

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public Nutrition getNutrition() {
        return nutrition;
    }

    public void setNutrition(Nutrition nutrition) {
        this.nutrition = nutrition;
    }

    public int getPortions() {
        return portions;
    }

    public void setPortions(int portions) {
        this.portions = portions;
    }

    public Rating getRating() {
        return rating;
    }

    public void setRating(Rating rating) {
        this.rating = rating;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getSource_url() {
        return source_url;
    }

    public void setSource_url(String source_url) {
        this.source_url = source_url;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public double getTotalTime() {
        return totalTime;
    }

    public void setTotalTime(double totalTime) {
        this.totalTime = totalTime;
    }

    public Long getUser_id() {
        return user_id;
    }

    public void setUser_id(Long user_id) {
        this.user_id = user_id;
    }

    public static class Ingredient {
        private String amount;
        private String name;
        private String unit;

        public Ingredient(){}

        public Ingredient(String amount, String name, String unit){
            this.amount = amount;
            this.name = name;
            this.unit = unit;
        }

        public String getUnit() {
            return unit;
        }

        public void setUnit(String unit) {
            this.unit = unit;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getAmount() {
            return amount;
        }

        public void setAmount(String amount) {
            this.amount = amount;
        }

        @Override
        public String toString() {
            return amount + " " + unit + " " + name;
        }
    }

    public static class Nutrition {
        private int kcal;

        public Nutrition(){
            this.kcal = 0;
        }

        public Nutrition(int kcal){
            this.kcal = kcal;
        }

        public int getKcal() {
            return kcal;
        }

        public void setKcal(int kcal) {
            this.kcal = kcal;
        }
    }

    public static class Rating {
        private int ratingCount;
        private double ratingValue;

        public Rating(){
            ratingCount = 0;
            ratingValue = 0;
        }

        public Rating(double ratingValue){
            this.ratingCount = 0;
            this.ratingValue = ratingValue;
        }

        public int getRatingCount() {
            return ratingCount;
        }

        public void setRatingCount(int ratingCount) {
            this.ratingCount = ratingCount;
        }

        public double getRatingValue() {
            return ratingValue;
        }

        public void setRatingValue(double ratingValue) {
            this.ratingValue = ratingValue;
        }
    }
}
