package com.springboot.vitalorganize;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@SpringBootApplication
@EnableScheduling
@EnableWebSecurity
public class VitalOrganizeApplication {

    public static void main(String[] args) {
        SpringApplication.run(VitalOrganizeApplication.class, args);
    }

    //Hier Steht nur ein Kommentar
}
