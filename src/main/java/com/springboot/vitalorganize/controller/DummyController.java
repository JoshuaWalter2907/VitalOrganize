package com.springboot.vitalorganize.controller;

import com.springboot.vitalorganize.model.Dummy;
import com.springboot.vitalorganize.service.DummyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class DummyController {

    private final DummyService dummyService;

    @Autowired
    public DummyController(DummyService dummyService) {
        this.dummyService = dummyService;
    }

    // GET-Mapping für die Test-Seite
    @GetMapping("/test")
    public String showTestPage() {
        return "test";  // Zeigt das 'test.html' Template an
    }

    // POST-Mapping für die Verarbeitung der Suchanfrage
    @PostMapping("/search")
    public String searchDummyById(@RequestParam("id") int id, Model model) {
        try {
            Dummy dummy = dummyService.getDummyById(id);
            model.addAttribute("dummy", dummy);
            return "testresponse";  // Zeigt das 'testresponse.html' Template an
        } catch (Exception e) {
            // Dummy nicht gefunden: Fehlernachricht hinzufügen
            model.addAttribute("errorMessage", "Kein Eintrag mit ID " + id + " gefunden.");
            return "test";  // Zeigt wieder 'test.html' mit Fehlermeldung an
        }
    }
}
