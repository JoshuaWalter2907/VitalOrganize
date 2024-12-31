package com.springboot.vitalorganize.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Getter
@Configuration
public class SpoonacularConfig {

    @Value("${spoonacular.apikey}")
    private String apiKey;

    @Bean
    public WebClient webClient() {
        return WebClient.builder().build();
    }



    public String getApiKey() {
        return apiKey;
    }
}
