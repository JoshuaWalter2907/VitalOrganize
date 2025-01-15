package com.springboot.vitalorganize.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
public class DeepTranslateConfig {
    @Value("${rapidApi-DeepTranslate.apikey}")
    private String translateApiKey;
}