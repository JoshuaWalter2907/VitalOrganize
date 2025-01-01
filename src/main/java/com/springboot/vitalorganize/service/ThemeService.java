package com.springboot.vitalorganize.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

@Service
public class ThemeService {

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
