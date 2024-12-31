package com.springboot.vitalorganize.controller;

import com.paypal.base.rest.PayPalRESTException;
import com.springboot.vitalorganize.dto.FundDetailsDto;
import com.springboot.vitalorganize.model.*;
import com.springboot.vitalorganize.repository.FundRepository;
import com.springboot.vitalorganize.repository.PaymentRepository;
import com.springboot.vitalorganize.repository.UserRepository;
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

@Controller
@AllArgsConstructor
@RequestMapping("/fund")
public class FundController {

    private final FundService fundService;
    private final UserService userService;


    private final PaypalService paypalService;
    private final FundRepository fundRepository;
    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;

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
            @RequestParam(required = false) Long amount,
            @RequestParam(name = "delete", required = false) Boolean delete
    ) {
        UserEntity currentUser = userService.getCurrentUser(user, authenticationToken);

        // Delegiere Logik an FundService
        FundDetailsDto fundDetails = fundService.getFundDetails(
                currentUser, id, query, username, reason, datefrom, dateto, amount, delete);

        // Model-Daten für View setzen
        model.addAttribute("loggedInUser", currentUser);
        model.addAttribute("funds", fundDetails.getFunds());
        model.addAttribute("myfunds", fundDetails.getMyFund());
        model.addAttribute("fundpayments", fundDetails.getFilteredPayments());
        model.addAttribute("balance", fundDetails.getBalance());
        model.addAttribute("delete", delete);
        model.addAttribute("show", show);
        model.addAttribute("error", fundDetails.getError());

        return "fund";
    }

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

        return "newfund";
    }

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
        // Benutzerinformationen aus der Authentication abrufen
        UserEntity userEntity = userService.getCurrentUser(user, authenticationToken);
        Long id = userEntity.getId();

        // Daten in die Session speichern
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

        // Aufruf der Service-Methode zur Zahlungsabwicklung und URL-Erstellung
        try {
            String approvalUrl = paypalService.createPaypalPayment(
                    Double.parseDouble(amount),
                    type,
                    description,
                    email,
                    fundid,
                    id
            );

            // Weiterleitung zur PayPal-Seite
            return new RedirectView(approvalUrl);

        } catch (PayPalRESTException e) {
            System.out.println(e.getMessage());
            return new RedirectView("/paypal/error");  // Fehlerseite zurückgeben
        }
    }

    @Transactional
    @GetMapping("/payinto/success")
    public String paypalSuccess(
            @RequestParam("paymentId") String paymentId,
            @RequestParam("PayerID") String payerId,
            @AuthenticationPrincipal OAuth2User user,
            OAuth2AuthenticationToken authentication,
            HttpSession session
    ) {

        // Daten aus der Session lesen
        Long userId = (Long) session.getAttribute("id");
        String type = (String) session.getAttribute("type");
        String amount = (String) session.getAttribute("amount");
        String currency = (String) session.getAttribute("currency");
        String description = (String) session.getAttribute("description");
        String receiverEmail = (String) session.getAttribute("email");
        Long fundId = (Long) session.getAttribute("fundid");
        Boolean delete = (Boolean) session.getAttribute("delete");

        // Benutzerinformationen verarbeiten
        String provider = authentication.getAuthorizedClientRegistrationId();
        String email = paypalService.getEmailForUser(user, provider);

        // Zahlungsabwicklung und Ergebnis erhalten
        try {
            // Den Service aufrufen, um die Zahlung zu verarbeiten
            paypalService.processPayment(
                    paymentId, payerId, type, amount, currency, description,
                    receiverEmail, email, provider, userId, fundId
            );

            // Falls die Löschoperation aktiviert ist, den Fund und die Zahlungen löschen
            if (delete != null && delete) {
                fundService.processFundDeletion(fundId);
            }

            return "redirect:/fund";  // Erfolgreiche Weiterleitung nach der Zahlung
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return "redirect:/fund/payinto/error";  // Fehlerseite anzeigen
        }
    }


    @GetMapping("/payinto/cancel")
    public String PaymentCancel(){
        return "redirect:/fund";
    }

    @GetMapping("/payinto/error")
    public String paymentError(){
        return "redirect:/fund";
    }

    @PostMapping("/create-fund")
    public String createFund(
            @RequestParam(name = "fundname") String fundname,
            @RequestParam(name = "selectedUsers", required = false) List<Long> userId,
            @ModelAttribute(name = "loggedInUser") UserEntity loggedInUser
    ) {
        // Logik wird jetzt an den FundService delegiert
        fundService.createFund(fundname, userId, loggedInUser);

        // Nach erfolgreicher Erstellung des Funds weiterleiten
        return "redirect:/fund";
    }


    @GetMapping("/edit-members")
    public String editMembers(
            @AuthenticationPrincipal OAuth2User user,
            OAuth2AuthenticationToken authenticationToken,
            @RequestParam(name = "fundId") Long fundId,
            @RequestParam(name = "query", required = false) String query,
            Model model
    ) {
        // Hole den aktuellen Benutzer
        UserEntity userEntity = userService.getCurrentUser(user, authenticationToken);

        // Hole den Fund basierend auf der fundId
        FundEntity fund = fundService.getFund(fundId);

        // Hole die gefilterten Freunde basierend auf der Anfrage
        List<UserEntity> filteredFriends = fundService.getFilteredFriends(userEntity, query);

        // Daten ins Model hinzufügen
        model.addAttribute("fund", fund);
        model.addAttribute("id", fundId);
        model.addAttribute("friends", filteredFriends);

        // Zur View weiterleiten
        return "edit-members";
    }


    @GetMapping("/delete-fund")
    public String deleteFund(
            @RequestParam(name = "fundId") Long id,
            @ModelAttribute(name = "loggedInUser") UserEntity loggedInUser,
            @RequestParam(name = "balance", required = false) String balance,
            HttpSession session,
            Model model
    ) {
        // Setze das "delete" Attribut in der Session
        session.setAttribute("delete", true);

        // Versuche den Fund zu löschen und überprüfe, ob der Benutzer der Admin ist
        boolean deleted = fundService.deleteFund(id, loggedInUser, balance);

        // Wenn der Fund gelöscht wurde, leite weiter
        if (deleted) {
            return "redirect:/fund";
        }

        // Andernfalls, gib den aktuellen Fund und die Balance im Model zurück
        FundEntity fund = fundRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Fund mit ID " + id + " nicht gefunden"));

        model.addAttribute("id", id);
        model.addAttribute("balance", fundService.getLatestFundBalance(fund));

        // Zur "delete-fund" View weiterleiten
        return "delete-fund";
    }

    @PostMapping("/edit-fund")
    public String editFund(
            @RequestParam("fundId") Long id,
            @RequestParam("selectedUsers") List<Long> users,
            @RequestParam("fundname") String name,
            @AuthenticationPrincipal OAuth2User user,
            OAuth2AuthenticationToken authentication
    ) {
        // Hole den aktuell angemeldeten Benutzer
        UserEntity loggedInUser = userService.getCurrentUser(user, authentication);

        // Bearbeite den Fund über den Service
        fundService.editFund(id, users, name, loggedInUser);

        // Weiterleitung zur Fund-Seite nach dem Bearbeiten
        return "redirect:/fund";
    }

}
