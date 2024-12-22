package com.springboot.vitalorganize.controller;

import com.paypal.api.payments.Links;
import com.paypal.api.payments.Payment;
import com.paypal.base.rest.PayPalRESTException;
import com.springboot.vitalorganize.model.*;
import com.springboot.vitalorganize.service.PaypalService;
import com.springboot.vitalorganize.service.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

import java.time.LocalDateTime;

 @AllArgsConstructor
@Slf4j
public class PaypalController {

    private PaypalService paypalService;
    private UserService userService;

    private final UserRepository userRepository;

    private final JavaMailSender mailSender;




    @GetMapping("/paypal")
    public String paypal(Model model) {
        model.addAttribute("balance", paypalService.getCurrentBalance());

        return "paypal";
    }

    @PostMapping("/paypal/create")
    public RedirectView createPaypal(
            @RequestParam("method") String method,
            @RequestParam("amount") String amount,
            @RequestParam("currency") String currency,
            @RequestParam("description") String description,
            @RequestParam("type") String type,
            @RequestParam("email") String email,
            @AuthenticationPrincipal OAuth2User user,
            HttpSession session
    ) {
        // Daten in die Session speichern
        session.setAttribute("amount", amount);
        session.setAttribute("currency", currency);
        session.setAttribute("description", description);
        session.setAttribute("type", type);
        session.setAttribute("email", email);
        session.setAttribute("method", method);

        try {
            String cancelUrl = "http://localhost:8080/paypal/cancel";
            String successUrl = "http://localhost:8080/paypal/success";

            String approvalUrl = paypalService.handlePaymentCreation(
                    Double.parseDouble(amount),
                    currency,
                    method,
                    description,
                    cancelUrl,
                    successUrl
            );

            return new RedirectView(approvalUrl);

        } catch (PayPalRESTException e) {
            System.out.println("Error");
            log.error("Error occurred while creating PayPal payment", e);
            return new RedirectView("/paypal/error");
        }
    }


    @GetMapping("/paypal/success")
    public RedirectView paypalSuccess(
            @RequestParam("paymentId") String paymentId,
            @RequestParam("PayerID") String payerId,
            @AuthenticationPrincipal OAuth2User user,
            OAuth2AuthenticationToken authentication,
            HttpSession session
    ) {
        // Daten aus der Session lesen
        String type = (String) session.getAttribute("type");
        String amount = (String) session.getAttribute("amount");
        String currency = (String) session.getAttribute("currency");
        String description = (String) session.getAttribute("description");
        String receiverEmail = (String) session.getAttribute("email");
        String method = (String) session.getAttribute("method");

        // Benutzerinformationen verarbeiten
        String provider = authentication.getAuthorizedClientRegistrationId();
        String email = paypalService.getEmailForUser(user, provider);

        // Zahlungsabwicklung und Ergebnis erhalten
        try {
            String redirectUrl = paypalService.processPayment(
                    paymentId, payerId, type, amount, currency, description,
                    receiverEmail, email, method, provider
            );
            return new RedirectView(redirectUrl);
        } catch (Exception e) {
            log.error("Error during PayPal success processing", e);
            System.out.println("Error2");
            return new RedirectView("/paypal/error");
        }
    }


    @GetMapping("/paypal/cancel")
    public String paypalCancel(){
        return "paypal";
    }

    @GetMapping("/paypal/error")
    public String paypalError(){
        return "paymentError";
    }

}
