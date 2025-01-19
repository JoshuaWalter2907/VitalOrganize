package com.springboot.vitalorganize.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.springboot.vitalorganize.config.DeepTranslateConfig;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Dieser Service ist zuständig für die Übersetzung von deutschen Zutatennamen
 * Diese Klasse bietet eine Methode zur Übersetzung
 */
@Service
@AllArgsConstructor
public class TranslationService {

    private DeepTranslateConfig deepTranslateConfig;
    private final WebClient webClient;

    /**
     * Übersetzt den Namen einer Zutat ins Englische
     * @param textToTranslate Der ursprüngliche Name der Zutat
     * @param sourceLang Die ursprünglich verwendete Sprache
     * @param targetLang Die gewünschte Sprache
     * @return Ins Englische übersetzter Name der Zutat
     */
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
