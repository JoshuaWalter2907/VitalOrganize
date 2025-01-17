package com.springboot.vitalorganize.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.vitalorganize.model.Recipe;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.List;

@Service
public class RecipeApiService {

    @Value("${rapidapi.key}")
    private String apiKey;
    @Value("${rapidapi.host}")
    private String apiHost;

    private final RestTemplate restTemplate;

    public RecipeApiService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<Recipe>  searchRecipes(String query) {
        // URL mit Query-Parameter
        String url = "https://gustar-io-deutsche-rezepte.p.rapidapi.com/search_api?text=" + query;

        // Headers für die API
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-RapidAPI-Host", apiHost);
        headers.set("X-RapidAPI-Key", apiKey);

        // Entity mit den Headers
        HttpEntity<String> entity = new HttpEntity<>(headers);

        // REST-Aufruf
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

        // Rückgabe des API-Ergebnisses
        return transformJson(response);
    }

    public List<Recipe> testCall(){
        List<Recipe> recipes = null;
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-RapidAPI-Key", "a44805f216msh77f9f452b9a1b36p1111f9jsn5e1c1092c133");
        headers.set("X-RapidAPI-Host", "gustar-io-deutsche-rezepte.p.rapidapi.com");

        HttpEntity<String> entity = new HttpEntity<>(headers);

        String url = "https://gustar-io-deutsche-rezepte.p.rapidapi.com/search_api?text=Schnitzel";
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

        System.out.println(response.getBody());

        return transformJson(response);
    }

    private List<Recipe> transformJson(ResponseEntity<String> response){
        List<Recipe> recipes = null;

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            recipes = objectMapper.readValue(response.getBody(), new TypeReference<List<Recipe>>() {});
            recipes.forEach(recipe -> System.out.println(recipe.getTitle()));
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("API Response Body: " + response.getBody());

        return recipes;
    }
}
