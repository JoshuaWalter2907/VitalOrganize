package com.springboot.vitalorganize.translation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;


public class MainController {

    private final TranslationManager translationManager;

    //@Autowired
    public MainController(TranslationManager translationManager) {
        this.translationManager = translationManager;
    }

    public void displayTitle() {
        System.out.println("Title: " + translationManager.getString("Main.title"));
    }
}
