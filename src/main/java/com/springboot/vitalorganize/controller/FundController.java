package com.springboot.vitalorganize.controller;

import com.paypal.base.rest.PayPalRESTException;
import com.springboot.vitalorganize.model.*;
import com.springboot.vitalorganize.service.PaypalService;
import com.springboot.vitalorganize.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.apache.el.lang.ELArithmetic;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@AllArgsConstructor
@RequestMapping("/fund")
public class FundController {

    private final PaypalService paypalService;
    private final FundRepository fundRepository;
    private final UserService userService;
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
            @RequestParam(required = false) String username,       // Für "Person suchen"
            @RequestParam(required = false) String reason,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate datefrom, // Startdatum
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateto,   // Enddatum
            @RequestParam(required = false) Long amount,
            @RequestParam(name="delete", required = false) Boolean delete

            ) {

        UserEntity userEntity = userService.getCurrentUser(user, authenticationToken);

        List<FundEntity> fundEntities = fundRepository.findFundsByUserId(userEntity.getId());
        Boolean error = false;

        if(query != null) {
            fundEntities = fundEntities.stream()
                    .filter(fundEntity -> fundEntity.getName().toLowerCase().contains(query.toLowerCase()))
                    .collect(Collectors.toList());
        }

        if(delete != null) {
            model.addAttribute("delete", delete);
        }

        if(id != null) {
            FundEntity fundEntity = fundRepository.findById(id).get();
            System.out.println("user is member: " + fundEntity.getUsers().contains(userEntity));
            if(!fundEntity.getUsers().contains(userEntity))
                error = true;
            List<Zahlung> fundpayments = fundEntity.getPayments();
            List<Zahlung> filteredfundpayments = paypalService.filterPayments(fundpayments, username, reason, datefrom, dateto, amount);

            model.addAttribute("fundpayments", filteredfundpayments);
        }
        if(show != null) {
            model.addAttribute("show", true);
        }

        System.out.println(userEntity);
        System.out.println(fundEntities);
        model.addAttribute("loggedInUser", userEntity);
        model.addAttribute("funds", fundEntities);
        model.addAttribute("balance", paypalService.getCurrentBalance());

        if(id != null) {
            FundEntity fund = fundRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Fund with ID " + id + " not found"));

            System.out.println(fund);
            model.addAttribute("myfunds", fund);
        }
        System.out.println("Error: " + error);
        model.addAttribute("error", error);

        return "fund";
    }

    @GetMapping("/newfund")
    public String newfund(
        @AuthenticationPrincipal OAuth2User user,
        OAuth2AuthenticationToken authenticationToken,
        @RequestParam(name = "query", required = false) String query,
        Model model
    ){
        UserEntity currentuser = userService.getCurrentUser(user, authenticationToken);
        List<UserEntity> friends = currentuser.getFriends();

        if(query != null) {
            friends = friends.stream()
                    .filter(friend -> friend.getUsername() != null &&
                            friend.getUsername().toLowerCase().contains(query.toLowerCase()))
                    .collect(Collectors.toList());
        }

        for(UserEntity friend : friends) {
            System.out.println(friend.getUsername());
        }
        model.addAttribute("friends", friends);

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
        UserEntity userEntity = userService.getCurrentUser(user, authenticationToken);
        Long id = userEntity.getId();

        String currency = "EUR";
        // Daten in die Session speichern
        session.setAttribute("amount", amount);
        session.setAttribute("currency", currency);
        session.setAttribute("description", description);
        session.setAttribute("type", type);
        session.setAttribute("id", id);
        session.setAttribute("redirect", request.getHeader("referer"));
        if(email != null)
            session.setAttribute("email", email);
        if(fundid != null)
            session.setAttribute("fundid", fundid);


        try {
            String cancelUrl = "http://localhost:8080/fund/payinto/cancel";
            String successUrl = "http://localhost:8080/fund/payinto/success";

            String approvalUrl = paypalService.handlePaymentCreation(
                    Double.parseDouble(amount),
                    currency,
                    description,
                    cancelUrl,
                    successUrl
            );

            return new RedirectView(approvalUrl);

        } catch (PayPalRESTException e) {
            System.out.println(e.getMessage());
            return new RedirectView("/paypal/error");
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
        Long id = (Long) session.getAttribute("id");
        String type = (String) session.getAttribute("type");
        String amount = (String) session.getAttribute("amount");
        String currency = (String) session.getAttribute("currency");
        String description = (String) session.getAttribute("description");
        String receiverEmail = (String) session.getAttribute("email");
        // Benutzerinformationen verarbeiten
        String provider = authentication.getAuthorizedClientRegistrationId();
        String email = paypalService.getEmailForUser(user, provider);
        Long fundid = (Long) session.getAttribute("fundid");
        Boolean delete = false;
        if(session.getAttribute("delete") != null)
            delete = (Boolean) session.getAttribute("delete");

        // Zahlungsabwicklung und Ergebnis erhalten
        try {
            paypalService.processPayment(
                    paymentId, payerId, type, amount, currency, description,
                    receiverEmail, email, provider, id, fundid
            );
            if(delete){
                FundEntity fund = fundRepository.findById(fundid).get();
                paymentRepository.deleteByFundId(fund.getId());
                fundRepository.delete(fund);
            }
            return "redirect:/fund";
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return "redirect:/fund/payinto/error";
        }
    }


    @GetMapping("/payinto/cancel")
    public String PaymentCancel(

    ){
        return "redirect:/fund";
    }

    @GetMapping("/payinto/error")
    public String paymentError(

    ){
        return "paymentError";
    }

    @PostMapping("/create-fund")
    public String createFund(
            @RequestParam(name = "fundname") String fundname,
            @RequestParam(name = "selectedUsers", required = false) List<Long> userId,
            @ModelAttribute(name = "loggedInUser") UserEntity loggedInUser
            ){

        FundEntity fund = new FundEntity();
        fund.setName(fundname);
        fund.setAdmin(loggedInUser);
        fund.getUsers().add(loggedInUser);
        if(userId != null){
            List<UserEntity> users = userRepository.findAllById(userId);
            for(UserEntity user : users) {
                fund.getUsers().add(user);
            }
        }
        fundRepository.save(fund);



        return  "redirect:/fund";
    }
    @GetMapping("/edit-members")
    public String editMembers(
            @AuthenticationPrincipal OAuth2User user,
            OAuth2AuthenticationToken authenticationToken,
            @RequestParam(name = "fundId") Long id,
            @RequestParam(name = "query", required = false) String query,
            Model model
    ){
        UserEntity userEntity = userService.getCurrentUser(user, authenticationToken);
        List<UserEntity> friends = userEntity.getFriends();

        if(query != null) {
            friends = friends.stream()
                    .filter(f -> f.getUsername().toLowerCase().contains(query.toLowerCase()))
                    .toList();
        }

        FundEntity fund = fundRepository.findById(id).get();
        model.addAttribute("id", id);
        model.addAttribute("friends", friends);
        model.addAttribute("fund", fund);
        return "edit-members";
    }

    @Transactional
    @GetMapping("/delete-fund")
    public String deleteFund(
        @RequestParam(name = "fundId") Long id,
        @ModelAttribute(name = "loggedInUser") UserEntity loggedInUser,
        @RequestParam(name = "balance", required = false) String balance,
        HttpSession session,
        Model model
    ){
        session.setAttribute("delete", true);
        FundEntity fund = fundRepository.findById(id).get();
        if(fund.getAdmin() != loggedInUser)
            return "redirect:/fund";

        if(balance != null){
            paymentRepository.deleteByFundId(fund.getId());
            fundRepository.delete(fund);
            return "redirect:/fund";
        }

        model.addAttribute("id", id);
        model.addAttribute("balance", fund.getPayments().getLast().getBalance());

        return "delete-fund";
    }

    @PostMapping("/edit-fund")
    public String editFund(
            @RequestParam("fundId") Long id,
            @RequestParam("selectedUsers") List<Long> users,
            @RequestParam("fundname") String name,
            @AuthenticationPrincipal OAuth2User user,
            OAuth2AuthenticationToken authentication
    ){
        UserEntity userEntity = userService.getCurrentUser(user, authentication);
        FundEntity fund = fundRepository.findById(id).get();
        List<UserEntity> friends = userRepository.findAllById(users);
        friends.add(userEntity);
        fund.setUsers(friends);
        fund.setName(name);
        fundRepository.save(fund);
        return "redirect:/fund";
    }

}
