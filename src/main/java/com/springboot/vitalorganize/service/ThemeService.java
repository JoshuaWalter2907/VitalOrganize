package com.springboot.vitalorganize.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

/**
 * Service-Klasse zur Verwaltung des Themes (Designs) der Anwendung.
 * Diese Klasse bietet eine Methode zur Ermittlung und Verwaltung des gewählten Themes,
 * das in der Benutzer-Session gespeichert wird.
 */
@Service
public class ThemeService {

    /**
     * Ruft das aktuelle Theme aus der Anfrage oder der Benutzer-Session ab.
     * Wenn ein Theme als Parameter übergeben wird, wird es in der Session gespeichert.
     * Falls kein Theme angegeben wird, wird das in der Session gespeicherte Theme verwendet oder auf "blue" zurückgegriffen.
     *
     * @param request Das HttpServletRequest-Objekt, das die Anfrage des Benutzers enthält
     * @return Das aktuelle Theme (Standard: "blue")
     */
    public String getTheme(HttpServletRequest request) {
        String theme = request.getParameter("theme");

        if (theme != null && !theme.isEmpty()) {
            request.getSession().setAttribute("theme", theme);
        }

        if (theme == null) {
            theme = (String) request.getSession().getAttribute("theme");
        }

        if (theme == null) {
            theme = "blue";
        }

        return theme;
    }
}

