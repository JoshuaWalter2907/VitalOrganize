package com.springboot.vitalorganize.controller;

import com.paypal.base.rest.PayPalRESTException;
import com.springboot.vitalorganize.entity.Profile_User.UserEntity;
import com.springboot.vitalorganize.model.Fund_Payment.*;
import com.springboot.vitalorganize.service.FundService;
import com.springboot.vitalorganize.service.PaypalService;
import com.springboot.vitalorganize.service.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.util.List;


/**
 * Controller für die Fund Endpoints
 */
@Controller
@AllArgsConstructor
@RequestMapping("/fund")
public class FundController {

    private final FundService fundService;
    private final PaypalService paypalService;
    private final UserService userService;


    /**
     * Endpoint für die Startseite der Funds
     * @param fundRequestDTO Informationen die benötigt werden
     * @param model Model für die View
     * @return Fund.html
     */
    @GetMapping()
    public String fund(
            FundRequestDTO fundRequestDTO,
            Model model
    ) {

        FundResponseDTO fundResponseDTO = fundService.getFundDetails(fundRequestDTO);

        model.addAttribute("fundData", fundResponseDTO);
        return "fund/fund";
    }

    /**
     * Endpoint um einen neuen Fund zu erstellen
     * @param newFundRequestDTO Benötigte Informationen
     * @param model Für die View im Frontend
     * @return newFund.html
     */
    @GetMapping("/newfund")
    public String newfund(
            NewFundRequestDTO newFundRequestDTO,
            Model model
    ) {
        List<UserEntity> filteredFriends = fundService.getFilteredFriends(newFundRequestDTO);

        model.addAttribute("friends", filteredFriends);
        return "fund/newfund";
    }


    /**
     * Endpoint um die Mitglieder eines Funds zu bearbeiten
     * @param newFundRequestDTO Informationen für die Bearbeitung
     * @param model Für die View im Frontend
     * @return edit-members.html
     */
    @GetMapping("/edit-members")
    public String editMembers(
            NewFundRequestDTO newFundRequestDTO,
            Model model
    ) {

        EditFundResponseDTO editFundResponseDTO = fundService.prepareEditFund(newFundRequestDTO);
        model.addAttribute("editFundData", editFundResponseDTO);
        return "fund/edit-members";
    }


    /**
     * Endpoint um einen Fund zu löschen
     * @param deleteFundRequestDTO Informationen die benötigt werden um einen Fund zu löschen
     * @param session zum Speichern eines delete Attributs
     * @param model Für die View im Frontend
     * @return delete-fund.html
     */
    @GetMapping("/delete-fund")
    public String deleteFund(
            DeleteFundRequestDTO deleteFundRequestDTO,
            HttpSession session,
            Model model
    ) {
        session.setAttribute("delete", true);

        DeleteFundResponseDTO deleteFundResponseDTO = fundService.prepareDeleteFund(deleteFundRequestDTO);
        model.addAttribute("DeleteFundData", deleteFundResponseDTO);

        if (deleteFundResponseDTO.getBalance() != 0) {
            return "fund/delete-fund";
        }
        return "redirect:/fund";
    }


    /**
     * Falls der Bezahlvorgang abgebrochen wird
     * @return Startseite der Funds
     */
    @GetMapping("/payinto/cancel")
    public String PaymentCancel(){
        return "redirect:/fund";
    }

    /**
     * Falls der Bezahlvorgang abgebrochen wird
     * @return Startseite der Funds
     */
    @GetMapping("/payinto/error")
    public String paymentError(){
        return "redirect:/fund";
    }


    /**
     * Endpoint um die erfolgreiche Zahlung zu verarbeiten
     * @param paymentSuccessRequestDTO benötigte Informationen von PayPal für die weitere verarbeitung
     * @param session Informationen aus der Session holen
     * @return Fundpage
     */
    @GetMapping("/payinto/success")
    public String paypalSuccess(
            PaymentSuccessRequestDTO paymentSuccessRequestDTO,
            HttpSession session
    ) {
        PaymentInformationSessionDTO paymentInformationSessionDTO = (PaymentInformationSessionDTO) session.getAttribute("paymentInformationSessionDTO");

        try {
            paypalService.processPayment(
                    userService.getCurrentUser(), paymentInformationSessionDTO, paymentSuccessRequestDTO
            );

            return "redirect:/fund";
        } catch (Exception e) {
            return "redirect:/fund";
        }
    }


    /**
     * Endpoint um Einzahlungen über die PayPal API zu tätigen
     * @param paymentInformationRequestDTO Informationen die die PayPal API aus dem Frontend benötigt
     * @param session zum speichern von Informationen
     * @return Weiterleitung zu Paypal oder im Fehlerfall zur Startseite
     */
    @PostMapping("/payinto")
    public RedirectView createPaypal(
            PaymentInformationRequestDTO paymentInformationRequestDTO,
            HttpSession session
    ) {
        PaymentInformationSessionDTO paymentInformationSessionDTO = fundService.preparePaymentInformationSessionDTO(paymentInformationRequestDTO);
        session.setAttribute("paymentInformationSessionDTO", paymentInformationSessionDTO);

        try {
            String approvalUrl = paypalService.createPaypalPayment(
                    paymentInformationSessionDTO
            );

            return new RedirectView(approvalUrl);
        } catch (PayPalRESTException e) {
            return new RedirectView("/");
        }
    }


    /**
     * Endpoint um einen neuen Fund zu erstellen
     * @param createFundRequestDTO Informationen die benötigt werden um einen neuen Fund zu erstellen
     * @return Redirect zur Startseite der Funds
     */
    @PostMapping("/create-fund")
    public String createFund(
            CreateFundRequestDTO createFundRequestDTO
    ) {
        fundService.createFund(createFundRequestDTO);

        return "redirect:/fund";
    }


    /**
     * Endpoint um einen Fund zu bearbeiten
     * @param editFundRequestDTO Informationen zu dem zu bearbeitenden Fund
     * @return Redirect zur Startseite der Funds
     */
    @PostMapping("/edit-fund")
    public String editFund(
            EditFundRequestDTO editFundRequestDTO
    ) {
        fundService.editFund(editFundRequestDTO);

        return "redirect:/fund";
    }

}


