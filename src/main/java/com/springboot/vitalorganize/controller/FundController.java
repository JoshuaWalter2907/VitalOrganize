package com.springboot.vitalorganize.controller;

import com.paypal.base.rest.PayPalRESTException;
import com.springboot.vitalorganize.dto.FundDetailsDto;
import com.springboot.vitalorganize.model.*;
import com.springboot.vitalorganize.repository.FundRepository;
import com.springboot.vitalorganize.service.FundService;
import com.springboot.vitalorganize.service.PaypalService;
import com.springboot.vitalorganize.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.time.LocalDate;
import java.util.List;


/**
 * Der FundController verwaltet alle Operationen, die mit der Verwaltung von "Funds" (z. B. Gruppenfinanzierungen)
 * in der Anwendung zusammenhängen. Dazu gehören das Anzeigen, Bearbeiten, Löschen und Bezahlen von Funds.
 *
 * Dieser Controller nutzt Spring MVC-Anmerkungen wie @Controller und @RequestMapping, um Endpunkte bereitzustellen,
 * und integriert Services für die Geschäftslogik.
 *
 * Hauptfunktionen:
 * - Anzeigen und Filtern von Funds
 * - Erstellen, Bearbeiten und Löschen von Funds
 * - Integration mit PayPal-Zahlungsdiensten
 *
 * Abhängigkeiten:
 * - FundService: Kernlogik für Fund-bezogene Operationen
 * - UserService: Benutzerverwaltung und Authentifizierung
 * - PaypalService: Zahlungsintegration mit PayPal
 */
@Controller
@AllArgsConstructor
@RequestMapping("/fund")
public class FundController {

    private final FundService fundService;
    private final UserService userService;
    private final PaypalService paypalService;

    /**
     * Zeigt die Hauptseite für Funds an, ermöglicht Filterung und Suchoptionen.
     *
     * @param model das UI-Modell
     * @param user der aktuelle OAuth2-Benutzer
     * @param authenticationToken das Authentifizierungstoken
     * @param id die Fund-ID (optional)
     * @param query Suchbegriff für die Filterung
     * @param show zeigt zusätzliche Optionen in der Ansicht an
     * @param username Name des Benutzers für Filterung (optional)
     * @param reason Grund für Fund (optional)
     * @param datefrom Filter: Startdatum (optional)
     * @param dateto Filter: Enddatum (optional)
     * @param amount Filter: Betrag (optional)
     * @return der Name der View
     */
    @GetMapping()
    public String fund(
            Model model,
            @AuthenticationPrincipal OAuth2User user,
            OAuth2AuthenticationToken authenticationToken,
            @RequestParam(name = "id", required = false) Long id,
            @RequestParam(name = "query", required = false) String query,
            @RequestParam(name = "show", required = false) Boolean show,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String reason,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate datefrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateto,
            @RequestParam(required = false) Long amount
    ) {
        UserEntity currentUser = userService.getCurrentUser(user, authenticationToken);

        // Delegiere Logik an FundService
        FundDetailsDto fundDetails = fundService.getFundDetails(
                currentUser, id, query, username, reason, datefrom, dateto, amount);

        // Model-Daten für View setzen
        model.addAttribute("loggedInUser", currentUser);
        model.addAttribute("funds", fundDetails.getFunds());
        model.addAttribute("myfunds", fundDetails.getMyFund());
        model.addAttribute("fundpayments", fundDetails.getFilteredPayments());
        model.addAttribute("balance", fundDetails.getBalance());
        model.addAttribute("show", show);
        model.addAttribute("error", fundDetails.getError());

        return "fund/fund";
    }

    /**
     * Zeigt die Seite zum Erstellen eines neuen Funds an.
     *
     * @param user der aktuelle OAuth2-Benutzer
     * @param authenticationToken das Authentifizierungstoken
     * @param query Suchbegriff für die Filterung von Freunden
     * @param model das UI-Modell
     * @return der Name der View
     */
    @GetMapping("/newfund")
    public String newfund(
            @AuthenticationPrincipal OAuth2User user,
            OAuth2AuthenticationToken authenticationToken,
            @RequestParam(name = "query", required = false) String query,
            Model model
    ) {
        UserEntity currentUser = userService.getCurrentUser(user, authenticationToken);

        // Delegation der Logik an den Service
        List<UserEntity> filteredFriends = fundService.getFilteredFriends(currentUser, query);

        // Ergebnisse an die View übergeben
        model.addAttribute("friends", filteredFriends);

        return "fund/newfund";
    }


    /**
     * Zeigt die Seite zum Bearbeiten der Mitglieder eines Funds an.
     *
     * @param user der aktuelle OAuth2-Benutzer
     * @param authenticationToken das Authentifizierungstoken
     * @param fundId die ID des Funds, dessen Mitglieder bearbeitet werden sollen
     * @param query Suchbegriff zur Filterung der Mitglieder
     * @param model das UI-Modell
     * @return der Name der View
     */
    @GetMapping("/edit-members")
    public String editMembers(
            @AuthenticationPrincipal OAuth2User user,
            OAuth2AuthenticationToken authenticationToken,
            @RequestParam(name = "fundId") Long fundId,
            @RequestParam(name = "query", required = false) String query,
            Model model
    ) {
        // Aktuellen Benutzer und Fund abrufen
        UserEntity userEntity = userService.getCurrentUser(user, authenticationToken);
        FundEntity fund = fundService.getFund(fundId);

        // Gefilterte Freunde abrufen
        List<UserEntity> filteredFriends = fundService.getFilteredFriends(userEntity, query);

        // Daten für die Ansicht vorbereiten
        model.addAttribute("fund", fund);
        model.addAttribute("id", fundId);
        model.addAttribute("friends", filteredFriends);

        return "fund/edit-members"; // Rückgabe der View
    }


    /**
     * Löscht einen Fund, falls der Kontostand null ist, oder zeigt eine Warnung an.
     *
     * @param id die ID des zu löschenden Funds
     * @param loggedInUser der aktuell eingeloggte Benutzer
     * @param balance der Kontostand des Funds (optional)
     * @param session die aktuelle HTTP-Session
     * @param model das UI-Modell
     * @return der Name der View oder eine Weiterleitung
     */
    @GetMapping("/delete-fund")
    public String deleteFund(
            @RequestParam(name = "fundId") Long id,
            @ModelAttribute(name = "loggedInUser") UserEntity loggedInUser,
            @RequestParam(name = "balance", required = false) String balance,
            HttpSession session,
            Model model
    ) {
        session.setAttribute("delete", true);
        FundEntity fund = fundService.getFund(id);

        // Überprüfen, ob der Fund-Kontostand null ist
        if (fundService.getLatestFundBalance(fund) != 0) {
            model.addAttribute("id", id);
            model.addAttribute("balance", fundService.getLatestFundBalance(fund));

            return "fund/delete-fund"; // Warnseite anzeigen
        }

        // Fund löschen, wenn der Kontostand null ist
        fundService.deleteFund(id, loggedInUser, balance);

        return "redirect:/fund"; // Zur Hauptseite weiterleiten
    }


    @GetMapping("/payinto/cancel")
    public String PaymentCancel(){
        return "redirect:/fund";
    }

    @GetMapping("/payinto/error")
    public String paymentError(){
        return "redirect:/fund";
    }


    /**
     * Verarbeitet die Rückmeldung einer erfolgreichen PayPal-Zahlung.
     *
     * @param paymentId die PayPal-Zahlungs-ID
     * @param payerId die ID des Zahlers
     * @param user der aktuelle OAuth2-Benutzer
     * @param authentication das Authentifizierungstoken
     * @param session die aktuelle HTTP-Session
     * @return eine Weiterleitung zur Hauptseite oder Fehlerseite
     */
    @Transactional
    @GetMapping("/payinto/success")
    public String paypalSuccess(
            @RequestParam("paymentId") String paymentId,
            @RequestParam("PayerID") String payerId,
            @AuthenticationPrincipal OAuth2User user,
            OAuth2AuthenticationToken authentication,
            HttpSession session
    ) {
        // Benutzer- und Zahlungsdaten abrufen
        UserEntity userEntity = userService.getCurrentUser(user, authentication);
        String email = userEntity.getEmail();
        Long userId = (Long) session.getAttribute("id");
        String type = (String) session.getAttribute("type");
        String amount = (String) session.getAttribute("amount");
        String currency = (String) session.getAttribute("currency");
        String description = (String) session.getAttribute("description");
        String receiverEmail = (String) session.getAttribute("email");
        Long fundId = (Long) session.getAttribute("fundid");
        String provider = authentication.getAuthorizedClientRegistrationId();

        try {
            // Zahlung verarbeiten
            paypalService.processPayment(
                    paymentId, payerId, type, amount, currency, description,
                    receiverEmail, email, provider, userId, fundId
            );

            return "redirect:/fund"; // Erfolgreiche Weiterleitung
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return "redirect:/fund"; // Fehlerseite anzeigen
        }
    }


    /**
     * Erstellt eine neue PayPal-Zahlung und leitet zur PayPal-Seite weiter.
     *
     * @param amount der Betrag der Zahlung
     * @param type der Typ der Zahlung (z. B. Spende oder Einzahlung)
     * @param description Beschreibung der Zahlung
     * @param email die E-Mail-Adresse des Empfängers (optional)
     * @param fundid die ID des Funds, dem die Zahlung zugeordnet ist (optional)
     * @param user der aktuelle OAuth2-Benutzer
     * @param authenticationToken das Authentifizierungstoken
     * @param session die aktuelle HTTP-Session
     * @param request die HTTP-Anfrage
     * @return eine Weiterleitung zur PayPal-Website oder Fehlerseite
     */
    @PostMapping("/payinto")
    public RedirectView createPaypal(
            @RequestParam("amount") String amount,
            @RequestParam("type") String type,
            @RequestParam("description") String description,
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "fundid", required = false) Long fundid,
            @AuthenticationPrincipal OAuth2User user,
            OAuth2AuthenticationToken authenticationToken,
            HttpSession session,
            HttpServletRequest request
    ) {
        // Benutzerinformationen abrufen
        UserEntity userEntity = userService.getCurrentUser(user, authenticationToken);
        Long id = userEntity.getId();

        // Session-Attribute setzen
        session.setAttribute("amount", amount);
        session.setAttribute("currency", "EUR");
        session.setAttribute("description", description);
        session.setAttribute("type", type);
        session.setAttribute("id", id);
        session.setAttribute("redirect", request.getHeader("referer"));
        if (email != null) {
            session.setAttribute("email", email);
        }
        if (fundid != null) {
            session.setAttribute("fundid", fundid);
        }

        try {
            // URL für die Zahlungsabwicklung abrufen
            String approvalUrl = paypalService.createPaypalPayment(
                    Double.parseDouble(amount),
                    type,
                    description,
                    email,
                    fundid,
                    id
            );

            return new RedirectView(approvalUrl); // Weiterleitung zu PayPal
        } catch (PayPalRESTException e) {
            System.out.println(e.getMessage());
            return new RedirectView("/paypal/error"); // Fehlerseite zurückgeben
        }
    }


    /**
     * Erstellt einen neuen Fund mit dem angegebenen Namen und den ausgewählten Benutzern.
     *
     * @param fundname der Name des neuen Funds
     * @param userId die IDs der Benutzer, die Mitglieder des Funds werden sollen
     * @param loggedInUser der aktuell eingeloggte Benutzer
     * @return eine Weiterleitung zur Hauptseite
     */
    @PostMapping("/create-fund")
    public String createFund(
            @RequestParam(name = "fundname") String fundname,
            @RequestParam(name = "selectedUsers", required = false) List<Long> userId,
            @ModelAttribute(name = "loggedInUser") UserEntity loggedInUser
    ) {
        // Fund erstellen
        fundService.createFund(fundname, userId, loggedInUser);

        return "redirect:/fund"; // Weiterleitung nach erfolgreicher Erstellung
    }



    /**
     * Bearbeitet einen bestehenden Fund und aktualisiert dessen Mitglieder und Namen.
     *
     * @param id die ID des Funds, der bearbeitet wird
     * @param users die IDs der neuen Mitglieder
     * @param name der neue Name des Funds
     * @param user der aktuelle OAuth2-Benutzer
     * @param authentication das Authentifizierungstoken
     * @return eine Weiterleitung zur Hauptseite
     */
    @PostMapping("/edit-fund")
    public String editFund(
            @RequestParam("fundId") Long id,
            @RequestParam("selectedUsers") List<Long> users,
            @RequestParam("fundname") String name,
            @AuthenticationPrincipal OAuth2User user,
            OAuth2AuthenticationToken authentication
    ) {
        // Aktuellen Benutzer abrufen
        UserEntity loggedInUser = userService.getCurrentUser(user, authentication);

        // Fund bearbeiten
        fundService.editFund(id, users, name, loggedInUser);

        return "redirect:/fund"; // Weiterleitung nach erfolgreicher Bearbeitung
    }

}
