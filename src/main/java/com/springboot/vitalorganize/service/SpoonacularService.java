package com.springboot.vitalorganize.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.springboot.vitalorganize.config.SpoonacularConfig;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Dieser Service holt die Daten von der Spoonacular-API
 * Diese Klasse bietet Methoden für den Zugriff auf die Spoonacular-API
 */
@Service
@AllArgsConstructor
public class SpoonacularService {

    private static final double USD_TO_EUR_CONVERSION_RATE = 0.97;
    private SpoonacularConfig spoonacularConfig;
    private WebClient webClient;

    /**
     * Gibt die Daten zu einer Zutat zurück
     * @param query Der Name der Zutat
     * @return Daten zu der Zutat
     */
    public Map<String, Object> getIngredientData(String query) throws IllegalArgumentException {
        String apiKey = spoonacularConfig.getSpoonacularApiKey();

        int ingredientId = getIngredientId(query, apiKey);

        return getIngredientInfo(ingredientId, apiKey);
    }

    /**
     * Gibt die Spoonacular-Id zu dem Namen einer Zutat zurück
     * @param query Der Name der Zutat
     * @param apiKey Der API-Key für die Spoonacular-API
     * @return Spoonacular-Id der Zutat
     */
    public int getIngredientId(String query, String apiKey) throws IllegalArgumentException {
        // fetches the first result that matches the query and returns the id
        String apiUrl = "https://api.spoonacular.com/food/ingredients/search?query=" + query + "&number=1&apiKey=" + apiKey;

        Mono<JsonObject> response = webClient.get()
                .uri(apiUrl)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String.class)
                .map(json -> JsonParser.parseString(json).getAsJsonObject())
                .log();
        JsonObject jsonObject = response.block();

        return extractIngredientId(jsonObject);
    }

    /**
     * Gibt die Spoonacular-Id aus der API-Response zurück
     * @param jsonObject Die API-Response
     * @return Spoonacular-Id
     */
    public int extractIngredientId(JsonObject jsonObject) throws IllegalArgumentException {
        // checks if results array exists and is not empty
        JsonArray results = jsonObject.getAsJsonArray("results");
        if (results == null || results.isEmpty()) {
            throw new IllegalArgumentException("Error: No result found to that query.");
        }

        // extracts the id of the first result
        JsonObject firstResult = results.get(0).getAsJsonObject();
        return firstResult.get("id").getAsInt();
    }

    /**
     * Gibt die Daten zu einer Zutat zurück
     * @param id Die Spoonacular-Id
     * @param apiKey Der API-Key für die Spoonacular-API
     * @return Map mit den Daten der Zutat
     */
    public Map<String, Object> getIngredientInfo(int id, String apiKey) {
        // fetches the information for the ingredient
        String apiUrl = "https://api.spoonacular.com/food/ingredients/" + id + "/information?amount=100&unit=g&currency=USD&apiKey=" + apiKey;

        Mono<JsonObject> response = webClient.get()
                .uri(apiUrl)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String.class)
                .map(json -> JsonParser.parseString(json).getAsJsonObject())
                .log();
        JsonObject ingredientInfo = response.block();

        return extractIngredientInfo(ingredientInfo);
    }

    /**
     * Gibt die Daten zu einer Zutat aus der API-Reponse zurück
     * @param jsonObject Die API-Response
     * @return Map mit den Daten der Zutat
     */
    public Map<String, Object> extractIngredientInfo(JsonObject jsonObject) {
        JsonElement aisle = jsonObject.get("aisle");
        String category = "unknown";

        // specific api value can be null in rare cases
        if(!aisle.isJsonNull()) {
            category = aisle.getAsString();
        }

        double estimatedCostInUSCents = jsonObject.getAsJsonObject("estimatedCost").get("value").getAsDouble();

        // converts cents to euros
        double estimatedCostInEuros = convertUsCentsToEuros(estimatedCostInUSCents);

        return Map.of(
                "category", category,
                "estimatedCostInEuros", estimatedCostInEuros
        );
    }

    /**
     * Konvertiert US-Cent-Preis zu Euro-Preis
     * @param centsAmount Der Preis in US Cents
     * @return Preis in Euro
     */
    private static double convertUsCentsToEuros(double centsAmount) {
        return centsAmount / 100 * USD_TO_EUR_CONVERSION_RATE;
    }
}
