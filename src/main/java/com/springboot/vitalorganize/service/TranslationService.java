package com.springboot.vitalorganize.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.springboot.vitalorganize.config.SpoonacularConfig;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@AllArgsConstructor
public class TranslationService {

    private SpoonacularConfig spoonacularConfig;
    private final WebClient webClient;

    public String translateQuery(String textToTranslate, String sourceLang, String targetLang) {
        String apiUrl = "https://translation-api.translate.com/translate/v1/mt";
        String translateApiKey = spoonacularConfig.getTranslateApiKey();

        try {
            Mono<String> responseMono = webClient.post()
                    .uri(apiUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("x-api-key", translateApiKey)
                    .bodyValue("source_language=" + sourceLang +
                            "&translation_language=" + targetLang +
                            "&text=" + textToTranslate)
                    .retrieve()
                    .bodyToMono(String.class);

            String response = responseMono.block();
            System.out.println(response);
            JsonObject jsonObject = JsonParser.parseString(response).getAsJsonObject();

            return jsonObject.get("translation").getAsString();
        } catch (Exception e) {
            return "Translation failed: " + e.getMessage();
        }
    }
}
