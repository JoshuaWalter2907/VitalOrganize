package com.springboot.vitalorganize.controller;
import com.springboot.vitalorganize.model.*;
import com.springboot.vitalorganize.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import java.util.Map;


/**
 * Der MainController steuert grundlegende Routen der Anwendung.
 *
 * Dieser Controller enthält Endpunkte für die Startseite, API-Dokumentation,
 * Theme-Änderungen, Login-Seite sowie die Verarbeitung von Zwei-Faktor-Authentifizierung (2FA).
 */
@Controller
@AllArgsConstructor
public class MainController {

    private final UserService userService;
    private final AuthenticationService authenticationService;
    private final TwoFactorService twoFactorService;
    private final SenderService senderService;


    /**
     * Rendert die Startseite der Anwendung.
     *
     * @param model das UI-Modell, in das die Benutzerattribute eingefügt werden
     * @return der Name der View für die Startseite
     */
    @RequestMapping("/")
    public String home(Model model) {
        authenticationService.getAuthenticatedUsername()
                .ifPresent(username -> model.addAttribute("username", username));
        return "home";
    }

    /**
     * Zeigt die API-Dokumentation an.
     *
     * @param model das UI-Modell
     * @return der Name der View für die API-Dokumentation
     */
    @GetMapping("/api-docs")
    public String apiDocs(Model model) {
        return "api/api-docs";
    }

    /**
     * Ändert das aktuelle Theme der Anwendung.
     *
     * @param request die aktuelle HTTP-Anfrage
     * @return eine Weiterleitung zur vorherigen Seite oder zur Startseite
     */
    @GetMapping("/change-theme")
    public String changeTheme(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer != null) {
            return "redirect:" + referer;
        }
        return "redirect:/";
    }

    /**
     * Ändert die aktuelle Sprache der Anwendung.
     *
     * @param request die aktuelle HTTP-Anfrage
     * @return eine Weiterleitung zur vorherigen Seite oder zur Startseite
     */
    @GetMapping("/change-lang")
    public String changeLang(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer != null) {
            return "redirect:" + referer;
        }
        return "redirect:/";
    }

    /**
     * Zeigt die Login-Seite an.
     *
     * @param model das UI-Modell
     * @return der Name der View für die Login-Seite
     */
    @GetMapping("/login")
    public String login(Model model) {
        return "LoginPage";
    }


    /**
     * Sendet einen Zwei-Faktor-Authentifizierungscode (2FA) an die E-Mail des Benutzers.
     *
     * @param email die Ziel-E-Mail-Adresse (optional)
     * @param inputString eine zusätzliche Eingabe (optional)
     * @param birthDate das Geburtsdatum (optional)
     * @param isPublic ein öffentlicher Indikator (optional)
     * @param user der aktuell authentifizierte Benutzer
     * @param auth2AuthenticationToken das Authentifizierungs-Token des Benutzers
     * @param session die aktuelle HTTP-Session
     * @return eine Weiterleitung zur vorherigen URI mit dem 2FA-Flag
     */
    @PostMapping("/send-2fa-code")
    public String sendTwoFactorCode(
            @RequestParam(name = "email", required = false) String email,
            @RequestParam(required = false) String inputString,
            @RequestParam(required = false) String birthDate,
            @RequestParam(required = false) Boolean isPublic,
            @AuthenticationPrincipal OAuth2User user,
            OAuth2AuthenticationToken auth2AuthenticationToken,
            HttpSession session
    ) {
        String uri = (String) session.getAttribute("uri");

        // Attribute in der Session setzen, wenn der Benutzer aus der Profilseite kommt
        if ("/profileaddition".equals(uri)) {
            session.setAttribute("email", email);
            session.setAttribute("inputString", inputString);
            session.setAttribute("birthDate", birthDate);
            session.setAttribute("isPublic", isPublic);
        }

        // Aktuelle Benutzerinformationen abrufen
        UserEntity userEntity = userService.getCurrentUser(user, auth2AuthenticationToken);
        if (email == null) {
            email = userEntity.getEmail();
        }

        // Alternative E-Mail für bestimmte Provider verwenden
        if ("github".equals(userEntity.getProvider()) && userEntity.getSendtoEmail() != null) {
            email = userEntity.getSendtoEmail();
        }

        // Zwei-Faktor-Code generieren und senden
        twoFactorService.generateAndSendCode(userEntity, email);

        return "redirect:" + uri + "?fa=true";
    }

    /**
     * Überprüft den eingegebenen Zwei-Faktor-Authentifizierungscode.
     *
     * @param user der aktuell authentifizierte Benutzer
     * @param auth2AuthenticationToken das Authentifizierungs-Token des Benutzers
     * @param digits die vom Benutzer eingegebenen Ziffern
     * @param session die aktuelle HTTP-Session
     * @return eine Weiterleitung basierend auf der URI und dem Verifizierungsstatus
     */
    @PostMapping("/verify-2fa")
    public String verifyTwoFactorCode(
            @AuthenticationPrincipal OAuth2User user,
            OAuth2AuthenticationToken auth2AuthenticationToken,
            @RequestParam Map<String, String> digits,
            HttpSession session
    ) {
        String email = "";
        String username = "";
        String birthdate = "";
        Boolean isPublic = false;

        String uri = (String) session.getAttribute("uri");
        if(uri.equals("/profileaddition")){
            email = session.getAttribute("email").toString();
            username = session.getAttribute("inputString").toString();
            birthdate = session.getAttribute("birthDate").toString();
            isPublic = (Boolean) session.getAttribute("isPublic");
        }

        // Aktuelle Benutzerinformationen abrufen
        UserEntity userEntity = userService.getCurrentUser(user, auth2AuthenticationToken);

        // Code verifizieren
        boolean isVerified = twoFactorService.verifyCode(user, auth2AuthenticationToken, digits, session);
        System.out.println(isVerified);

        // Verifizierungslogik und Weiterleitungen
        if (isVerified) {
            session.setAttribute("2fa_verified", true);
            if ("/profile-edit".equals(uri)) {
                senderService.createPdf(userEntity);
                return "redirect:/profile-edit";
            } else if ("/profileaddition".equals(uri)) {
                return "forward:/profileaddition?inputString=" + username + "&isPublic=" + isPublic + "&birthDate=" + birthdate + "&email=" + email;
            }
        }

        return "redirect:/error";
    }




}
