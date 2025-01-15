package com.springboot.vitalorganize.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.springboot.vitalorganize.config.DeepTranslateConfig;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@AllArgsConstructor
public class TranslationService {

    private DeepTranslateConfig deepTranslateConfig;
    private final WebClient webClient;

    public String translateQuery(String textToTranslate, String sourceLang, String targetLang) {
        String apiUrl = "https://deep-translate1.p.rapidapi.com/language/translate/v2";
        String rapidApiKey = deepTranslateConfig.getTranslateApiKey();

        Mono<String> responseMono = webClient.post()
            .uri(apiUrl)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .header("X-RapidAPI-Key", rapidApiKey)
            .header("X-RapidAPI-Host", "deep-translate1.p.rapidapi.com")
            .bodyValue("{\"q\":\"" + textToTranslate + "\",\"source\":\"" + sourceLang + "\",\"target\":\"" + targetLang + "\"}")
            .retrieve()
            .bodyToMono(String.class);

        String response = responseMono.block();
        JsonObject jsonObject = JsonParser.parseString(response).getAsJsonObject();

        String translatedIngredient;
        try{
            translatedIngredient = jsonObject.getAsJsonObject("data")
                    .getAsJsonObject("translations")
                    .get("translatedText")
                    .getAsString();
        } catch (NullPointerException e) {
            // translation failed
            throw new IllegalArgumentException("error.translationFailed");
        }
        return translatedIngredient;
    }
}
