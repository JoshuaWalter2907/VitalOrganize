package com.springboot.vitalorganize.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.springboot.vitalorganize.config.SpoonacularConfig;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
@AllArgsConstructor
public class SpoonacularService {

    private SpoonacularConfig spoonacularConfig;
    private WebClient webClient;

    public Map<String, Object> getIngredientData(String query) throws IllegalArgumentException {
        int ingredientId = getIngredientId(query);
        Map<String, Object> ingredientData = getIngredientInformation(ingredientId);

        return ingredientData;
    }

    public int getIngredientId(String query) throws IllegalArgumentException {
        String apiKey = spoonacularConfig.getSpoonacularApiKey();

        // fetch the first result that matches the query and return the id
        String apiUrl = "https://api.spoonacular.com/food/ingredients/search?query=" + query + "&number=1&apiKey=" + apiKey;

        // Fetch response and parse it directly into a JsonObject
        Mono<JsonObject> response = webClient.get()
                .uri(apiUrl)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String.class) // Get the raw JSON string
                .map(json -> JsonParser.parseString(json).getAsJsonObject()) // Parse it to JsonObject
                .log();

        // Block to retrieve the JsonObject and process it
        JsonObject jsonObject = response.block();

        return extractIngredientId(jsonObject);
    }

    public int extractIngredientId(JsonObject jsonObject) throws IllegalArgumentException {
        // Check if "results" array exists and is not empty
        JsonArray results = jsonObject.getAsJsonArray("results");
        if (results == null || results.isEmpty()) {
            throw new IllegalArgumentException("Error: No result found to that query.");
        }

        // Extract the ID of the first result
        JsonObject firstResult = results.get(0).getAsJsonObject();
        return firstResult.get("id").getAsInt();
    }

    public Map<String, Object> getIngredientInformation(int id) {
        String apiKey = spoonacularConfig.getSpoonacularApiKey();
        String apiUrl = "https://api.spoonacular.com/food/ingredients/" + id + "/information?amount=100&unit=g&currency=USD&apiKey=" + apiKey;

        // Fetch and parse the response directly
        Mono<JsonObject> response = webClient.get()
                .uri(apiUrl)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String.class) // Get raw JSON string
                .map(json -> JsonParser.parseString(json).getAsJsonObject()) // Parse to JsonObject
                .log();

        // Block to get the JsonObject and analyze it
        JsonObject ingredientInfo = response.block();
        // at this point the existence of the ingredient info is no longer in question
        return analyseIngredientData(ingredientInfo);
    }

    private static final double USD_TO_EUR_CONVERSION_RATE = 0.96;

    public Map<String, Object> analyseIngredientData(JsonObject jsonObject) {
        // Extract values from the JSON object
        String category = jsonObject.get("aisle").getAsString();
        double estimatedCostInCents = jsonObject.getAsJsonObject("estimatedCost").get("value").getAsDouble();

        // Convert cost to EUR
        double estimatedCostInEuros = convertUsCentsToEuros(estimatedCostInCents);

        // round cost to 2 digits
        estimatedCostInEuros = (double) Math.round(estimatedCostInEuros * 100) /100;

        // return a result map
        return Map.of(
                "category", category,
                "estimatedCostInEuros", estimatedCostInEuros
        );
    }

    private static double convertUsCentsToEuros(double usCentsAmount) {
        return usCentsAmount / 100 * USD_TO_EUR_CONVERSION_RATE;
    }
}
