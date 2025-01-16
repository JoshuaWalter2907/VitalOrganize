package com.springboot.vitalorganize.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
public class SpoonacularConfig {
    @Value("${spoonacular.apikey}")
    private String spoonacularApiKey;
}
