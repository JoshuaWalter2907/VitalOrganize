package com.springboot.vitalorganize.controller.General;
import com.springboot.vitalorganize.model.Profile.FARequestDTO;
import com.springboot.vitalorganize.model.Profile.Verify2FARequestDTO;
import com.springboot.vitalorganize.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;


/**
 * Der HomePageController steuert grundlegende Routen der Anwendung.
 * Er stellt die Funktionalität von 2FA Endpunkten dar
 */
@Controller
@AllArgsConstructor
public class HomePageController {

    public static final String REDIRECT = "redirect:";
    private final UserService userService;
    private final AuthenticationService authenticationService;
    private final TwoFactorService twoFactorService;
    private final SenderService senderService;


    /**
     * Rendert die Startseite der Anwendung.
     *
     * @param model Um Attribute an das Frontend zu geben
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
     * @param model Um Attribute an das Frontend zu geben
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
            return REDIRECT + referer;
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
            return REDIRECT + referer;
        }
        return "redirect:/";
    }

    /**
     * Zeigt die Login-Seite an.
     *
     * @return der Name der View für die Login-Seite
     */
    @GetMapping("/login")
    public String login() {
        return "LoginPage";
    }


    /**
     * Endpoint um einen 2FA Code an den Benutzer zu senden
     * @param faRequestDTO 2FA Request DTO mit den relevanten Informationen
     * @param session Session um Attribute zu speichern
     * @return Redirect auf die entsprechende Seite
     */
    @PostMapping("/send-2fa-code")
    public String sendTwoFactorCode(
            FARequestDTO faRequestDTO,
            HttpSession session
    ) {
        String uri = (String) session.getAttribute("uri");

        if ("/additional-registration".equals(uri)) {
            session.setAttribute("email", faRequestDTO.getEmail());
            session.setAttribute("username", faRequestDTO.getUsername());
            session.setAttribute("birthday", faRequestDTO.getBirthday());
        }

        twoFactorService.generateAndSendCode(faRequestDTO);

        return "redirect:" + uri + "?fa=true";
    }

    /**
     * Endpooint um eine 2FA Anfrage zu verifizieren
     * @param verify2FARequestDTO DTO mit allen Informationen, die benötigt werden um zu verifizieren
     * @param session Um Attribute aus der Session zu holen
     * @return Redirect auf die entsprechende Seite nach der Verifizierung oder bei falscher Verifizierung
     */
    @PostMapping("/verify-2fa")
    public String verifyTwoFactorCode(
            Verify2FARequestDTO verify2FARequestDTO,
            HttpSession session
    ) {
        String email = "";
        String username = "";
        String birthdate = "";

        String uri = session.getAttribute("uri").toString();
        if(uri.equals("/additional-registration")){
            email = session.getAttribute("email").toString();
            username = session.getAttribute("username").toString();
            birthdate = session.getAttribute("birthday").toString();
        }

        boolean isVerified = twoFactorService.verifyCode(verify2FARequestDTO.getDigits(), session);


        if (isVerified) {
            session.setAttribute("2fa_verified", true);
            if ("/profile-edit".equals(uri)) {
                senderService.createPdf(userService.getCurrentUser());
                return "redirect:/profile-edit";
            } else if ("/additional-registration".equals(uri)) {
                return "forward:/additional-registration?username=" + username + "&birthday=" + birthdate + "&email=" + email;
            }
        }
        return "redirect:/";
    }

}
