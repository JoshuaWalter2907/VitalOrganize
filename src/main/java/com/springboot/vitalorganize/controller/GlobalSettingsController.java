package com.springboot.vitalorganize.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class GlobalSettingsController {

    @PostMapping("/change-theme")
    public String changeTheme(
            @RequestParam(value = "theme") String theme,
            HttpSession session,
            HttpServletRequest request) {

        // save theme into the session
        if (theme != null) {
            session.setAttribute("theme", theme);
        }

        // request.getHeader("Referer") gets the url from the most recent page, where the request was sent
        String requestUrl = request.getHeader("Referer");

        return "redirect:" + requestUrl;
    }

    @PostMapping("/change-lang")
    public String changeLanguage(
            @RequestParam(value = "lang") String lang,
            HttpSession session,
            HttpServletRequest request) {

        // save language into the session
        if (lang != null) {
            session.setAttribute("lang", lang);
        }

        // request.getHeader("Referer") gets the url from the most recent page, where the request was sent
        String requestUrl = request.getHeader("Referer");

        return "redirect:" + requestUrl;
    }
}
