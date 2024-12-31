package com.springboot.vitalorganize.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

@Service
public class ThemeService {

    public String getTheme(HttpServletRequest request) {
        String theme = request.getParameter("theme");

        // Wenn der theme-Parameter vorhanden ist, speichere ihn in der Session
        if (theme != null && !theme.isEmpty()) {
            request.getSession().setAttribute("theme", theme);
        }

        // Wenn kein theme-Parameter gesetzt ist, prüfe, ob ein Theme in der Session existiert
        if (theme == null) {
            theme = (String) request.getSession().getAttribute("theme");
        }

        // Falls kein Theme gesetzt ist, verwende das Standard-Theme (z.B. "light")
        if (theme == null) {
            theme = "blue"; // Default-Theme
        }

        return theme;
    }
}
