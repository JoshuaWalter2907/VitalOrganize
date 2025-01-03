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
    private String spoonacularApiKey;

    @Value("${translate.apikey}")
    private String translateApiKey;

    @Bean
    public WebClient webClient() {
        return WebClient.builder().build();
    }
}
