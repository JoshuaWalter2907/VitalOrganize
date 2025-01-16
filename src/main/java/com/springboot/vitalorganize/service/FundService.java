package com.springboot.vitalorganize.service;

import com.springboot.vitalorganize.entity.Fund_Payments.FundEntity;
import com.springboot.vitalorganize.entity.Fund_Payments.PaymentEntity;
import com.springboot.vitalorganize.entity.Profile_User.UserEntity;
import com.springboot.vitalorganize.model.Fund_Payment.*;
import com.springboot.vitalorganize.repository.FundRepository;
import com.springboot.vitalorganize.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Service zur Verwaltung von Fonds, deren Benutzern und Zahlungen.
 * Bietet Methoden zum Abrufen von Fondsdaten, Erstellen, Bearbeiten, Löschen und Berechnen von Fonds.
 */
@Service
@AllArgsConstructor
public class FundService {


    private final UserRepository userRepository;
    private final FundRepository fundRepository;
    private final PaypalService paypalService;
    private final UserService userService;


    public FundResponseDTO getFundDetails(FundRequestDTO fundRequestDTO) {
        FundResponseDTO fundResponseDTO = new FundResponseDTO();
        UserEntity user = userService.getCurrentUser();
        boolean error = false;

        List<FundEntity> funds = fundRepository.findByUsers_Id(user.getId());
        if (fundRequestDTO.getQuery() != null) {
            funds = funds.stream()
                    .filter(f -> f.getName().toLowerCase().contains(fundRequestDTO.getQuery().toLowerCase()))
                    .toList();
        }

        List<PaymentEntity> filteredPayments = new ArrayList<>();
        FundEntity myFund = null;

        if (fundRequestDTO.getId() != null) {
            FundEntity fund = fundRepository.findById(fundRequestDTO.getId()).orElse(null);
            if(fund == null) {
                throw new IllegalArgumentException(String.valueOf(fundRequestDTO.getId()));
            }
            if (!fund.getUsers().contains(user)) {
                error = true;
            } else {
                myFund = fund;
                filteredPayments = paypalService.filterPayments(
                        fund.getPayments(), fundRequestDTO.getUsername(), fundRequestDTO.getReason(), fundRequestDTO.getDatefrom(), fundRequestDTO.getDateto(), fundRequestDTO.getAmount());
            }
        }

        int pageNumber = fundRequestDTO.getPage();
        int pageSize = fundRequestDTO.getSize();
        fundResponseDTO.setPageSize(pageSize);

        int totalPayments = filteredPayments.size();
        int totalPages = (int) Math.ceil((double) totalPayments / pageSize);

        int startIndex = pageNumber * pageSize;
        int endIndex = Math.min(startIndex + pageSize, totalPayments);

        List<PaymentEntity> pagedPayments = filteredPayments.subList(startIndex, endIndex);

        fundResponseDTO.setLoggedInUser(user);
        fundResponseDTO.setFunds(funds);
        fundResponseDTO.setMyfunds(myFund);
        fundResponseDTO.setFundpayments(pagedPayments);
        fundResponseDTO.setBalance(paypalService.getCurrentBalance());
        fundResponseDTO.setError(error);
        fundResponseDTO.setShow(fundRequestDTO.isShow());
        fundResponseDTO.setTotalPayments(totalPayments);
        fundResponseDTO.setTotalPages(totalPages);
        fundResponseDTO.setPageNumber(pageNumber);
        fundResponseDTO.setUsername(fundRequestDTO.getUsername());
        fundResponseDTO.setReason(fundRequestDTO.getReason());
        fundResponseDTO.setDatefrom(fundRequestDTO.getDatefrom());
        fundResponseDTO.setDateto(fundRequestDTO.getDateto());
        fundResponseDTO.setAmount(fundRequestDTO.getAmount());

        return fundResponseDTO;
    }

    public List<UserEntity> getFilteredFriends(NewFundRequestDTO newFundRequestDTO) {
        UserEntity currentUser = userService.getCurrentUser();
        List<UserEntity> friends = currentUser.getFriends();

        // Falls ein Query-Parameter gesetzt ist, filtere die Freunde
        if (newFundRequestDTO.getQuery() != null && !newFundRequestDTO.getQuery().isBlank()) {
            friends = friends.stream()
                    .filter(friend -> friend.getUsername() != null &&
                            friend.getUsername().toLowerCase().contains(newFundRequestDTO.getQuery().toLowerCase()))
                    .toList();
        }

        return friends;
    }


    public void createFund(CreateFundRequestDTO createFundRequestDTO) {
        UserEntity loggedInUser = userService.getCurrentUser();
        FundEntity fund = new FundEntity();
        fund.setName(createFundRequestDTO.getFundname());
        fund.setAdmin(loggedInUser);
        fund.getUsers().add(loggedInUser);

        if (createFundRequestDTO.getSelectedUsers() != null) {
            List<UserEntity> users = userRepository.findAllById(createFundRequestDTO.getSelectedUsers());
            fund.getUsers().addAll(users);
        }

        fundRepository.save(fund);
    }

    /**
     * Holt einen Fund anhand seiner ID.
     *
     * @param fundId die ID des Fonds
     * @return der Fund mit der angegebenen ID
     */
    public FundEntity getFund(Long fundId) {
        return fundRepository.findById(fundId).orElse(null);
    }

    /**
     * Überprüft, ob ein Benutzer der Administrator eines Fonds ist.
     *
     * @param user der zu überprüfende Benutzer
     * @param fund der Fund, für den überprüft werden soll
     * @return true, wenn der Benutzer der Administrator des Fonds ist, andernfalls false
     */
    private boolean isUserAdminOfFund(UserEntity user, FundEntity fund) {
        return fund.getAdmin().equals(user);
    }


    @Transactional
    public void editFund(EditFundRequestDTO editFundRequestDTO) {
        UserEntity loggedInUser = userService.getCurrentUser();
        FundEntity fund = fundRepository.findById(editFundRequestDTO.getFundId()).orElse(null);

        if(fund == null) {
            throw new IllegalArgumentException(String.valueOf(editFundRequestDTO.getFundId()));
        }

        List<UserEntity> users = userRepository.findAllById(editFundRequestDTO.getSelectedUsers());
        if (!users.contains(loggedInUser)) {
            users.add(loggedInUser); // Füge den Admin hinzu
        }

        if(editFundRequestDTO.getFundname() != null && !editFundRequestDTO.getFundname().isBlank()) {
            fund.setName(editFundRequestDTO.getFundname());
        }
        fund.setUsers(users);

        fundRepository.save(fund);
    }

    /**
     * Holt den aktuellen Kontostand eines Fonds.
     *
     * @param fund der Fund, für den der Kontostand abgerufen werden soll
     * @return der aktuelle Kontostand des Fonds
     */
    public double getLatestFundBalance(FundEntity fund) {
        if (fund.getPayments().isEmpty()) {
            return 0.0;
        }
        return fund.getPayments().getLast().getBalance();
    }

    /**
     * Löscht einen Fund aus der Datenbank. Nur der Administrator des Fonds kann diesen Vorgang durchführen.
     * Dieser Vorgang wird in einer Transaktion ausgeführt.
     *
     * @param fundId       die ID des Fonds, der gelöscht werden soll
     * @param loggedInUser der aktuell angemeldete Benutzer, der den Fund löschen möchte
     * @param balance      der aktuelle Kontostand des Fonds
     * @throws SecurityException wenn der Benutzer nicht der Administrator des Fonds ist
     */
    @Transactional
    public void deleteFund(Long fundId, UserEntity loggedInUser, String balance) {
        FundEntity fund = fundRepository.findById(fundId).orElse(null);

        if(fund == null) {
            throw new IllegalArgumentException(String.valueOf(fundId));
        }

        if (!isUserAdminOfFund(loggedInUser, fund)) {
            throw new SecurityException("Nicht autorisiert, um den Fund zu löschen.");
        }

        fundRepository.delete(fund);
    }

    public EditFundResponseDTO prepareEditFund(NewFundRequestDTO newFundRequestDTO) {
        EditFundResponseDTO editFundResponseDTO = new EditFundResponseDTO();

        FundEntity fund = getFund(newFundRequestDTO.getFundId());

        List<UserEntity> filteredUsers = getFilteredFriends(newFundRequestDTO);

        editFundResponseDTO.setFund(fund);
        editFundResponseDTO.setFriends(filteredUsers);
        editFundResponseDTO.setId(fund.getId());
        editFundResponseDTO.setQuery(newFundRequestDTO.getQuery());

        return editFundResponseDTO;
    }

    @Transactional
    public DeleteFundResponseDTO prepareDeleteFund(DeleteFundRequestDTO deleteFundRequestDTO) {
        DeleteFundResponseDTO deleteFundResponseDTO = new DeleteFundResponseDTO();

        FundEntity fund = getFund(deleteFundRequestDTO.getFundId());
        if(getLatestFundBalance(fund) != 0) {
            deleteFundResponseDTO.setId(deleteFundResponseDTO.getId());
            deleteFundResponseDTO.setBalance(getLatestFundBalance(fund));
            return deleteFundResponseDTO;
        }else {
            deleteFund(deleteFundRequestDTO.getFundId(), userService.getCurrentUser(), deleteFundRequestDTO.getBalance());
            return deleteFundResponseDTO;
        }
    }

    public PaymentInformationSessionDTO preparePaymentInformationSessionDTO(PaymentInformationRequestDTO paymentInformationRequestDTO) {
        PaymentInformationSessionDTO paymentInformationSessionDTO = new PaymentInformationSessionDTO();
        paymentInformationSessionDTO.setAmount(paymentInformationRequestDTO.getAmount());
        paymentInformationSessionDTO.setDescription(paymentInformationRequestDTO.getDescription());
        paymentInformationSessionDTO.setType(paymentInformationRequestDTO.getType());
        paymentInformationSessionDTO.setId(userService.getCurrentUser().getId());
        if(paymentInformationRequestDTO.getEmail() != null)
            paymentInformationSessionDTO.setReceiverEmail(paymentInformationRequestDTO.getEmail());
        if (paymentInformationRequestDTO.getFundid() != null)
            paymentInformationSessionDTO.setFundid(paymentInformationRequestDTO.getFundid());

        return paymentInformationSessionDTO;
    }
}
