package com.springboot.vitalorganize.controller;

import com.paypal.api.payments.Links;
import com.paypal.api.payments.Payment;
import com.paypal.base.rest.PayPalRESTException;
import com.springboot.vitalorganize.model.PaymentType;
import com.springboot.vitalorganize.model.UserRepository;
import com.springboot.vitalorganize.model.UserEntity;
import com.springboot.vitalorganize.model.Zahlung;
import com.springboot.vitalorganize.service.PaypalService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

import java.time.LocalDateTime;

@Controller
@RequiredArgsConstructor
@Slf4j
public class PaypalController {

    private final PaypalService paypalService;

    @Autowired
    private UserRepository userRepository;


    @GetMapping("/paypal")
    public String paypal() {
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
            HttpSession session
    ){

        session.setAttribute("amount", amount);
        session.setAttribute("currency", currency);
        session.setAttribute("description", description);
        session.setAttribute("type", type);
        session.setAttribute("email", email);

        try {
            String cancleUrl = "http://localhost:8080/paypal/cancel";
            String successUrl = "http://localhost:8080/paypal/success";
            Payment payment= paypalService.createPayment(
                    Double.parseDouble(amount),
                    currency,
                    method,
                    "sale",
                    description,
                    cancleUrl,
                    successUrl
            );

            for(Links links: payment.getLinks()){
                if(links.getRel().equals("approval_url")){
                    return new RedirectView(links.getHref());
                }
            }
        }catch (PayPalRESTException e){
            log.error("error occured" ,e);
        }
        return new RedirectView("/paypal/error");
    }

    @GetMapping("/paypal/success")
    public String paypalSuccess(
            @RequestParam("paymentId") String paymentId,
            @RequestParam("PayerID") String payerId,
            @AuthenticationPrincipal OAuth2User user,
            OAuth2AuthenticationToken authentication,
            HttpSession session
            )
    {
        String type = session.getAttribute("type").toString();
        String amount = (String) session.getAttribute("amount");
        String currency = session.getAttribute("currency").toString();
        String description = session.getAttribute("description").toString();
        String provider = authentication.getAuthorizedClientRegistrationId();
        String receiverEmail = session.getAttribute("email").toString();
        String email = user.getAttribute("email");

        if (provider.equals("github")) {
            String username = user.getAttribute("login");
            System.out.println(username);
            email = username + "@github.com"; // Dummy-E-Mail erstellen
            System.out.println(email);
        }

        UserEntity existingUser = userRepository.findByEmailAndProvider(email, provider);
        existingUser.setRole("MEMBER");
        userRepository.save(existingUser);

        try {
            Zahlung zahlung = new Zahlung();
            zahlung.setDate(LocalDateTime.now());
            zahlung.setReason(description);
            zahlung.setAmount(Double.valueOf(amount));
            zahlung.setCurrency(currency);
            zahlung.setUser(existingUser);

            if("EINZAHLEN".equalsIgnoreCase(type)){
                zahlung.setType(PaymentType.EINZAHLEN);
                Payment payment = paypalService.executePayment(paymentId, payerId);
                if(payment.getState().equals("approved")){
                    paypalService.addPayment(zahlung);
                    return "subscriptionSuccess";
                }
            } else if ("AUSZAHLEN".equalsIgnoreCase(type)) {
                double balance = paypalService.getCurrentBalance();
                if(Double.valueOf(amount) > balance){
                    return "paymentError";
                }else{
                    zahlung.setType(PaymentType.AUSZAHLEN);
                    paypalService.executePayout(receiverEmail, amount,currency);
                    paypalService.addPayment(zahlung);
                    return "subscriptionSuccess";
                }
            }


        }catch (PayPalRESTException e){
            log.error("error occured" ,e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return "subscriptionSuccess";
    }

    @GetMapping("/paypal/cancel")
    public String paypalCancel(){
        return "paymentCancel";
    }

    @GetMapping("/paypal/error")
    public String paypalError(){
        return "paymentError";
    }

}
