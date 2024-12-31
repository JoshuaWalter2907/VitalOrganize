package com.springboot.vitalorganize.service;

import com.springboot.vitalorganize.dto.FundDetailsDto;
import com.springboot.vitalorganize.model.*;
import com.springboot.vitalorganize.service.repositoryhelper.FundRepositoryService;
import com.springboot.vitalorganize.service.repositoryhelper.PaymentRepositoryService;
import com.springboot.vitalorganize.service.repositoryhelper.UserRepositoryService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class FundService {

    private final FundRepositoryService fundRepositoryService;
    private final PaymentRepositoryService paymentRepositoryService;
    private final UserRepositoryService userRepositoryService;
    private final PaypalService paypalService;


    public FundDetailsDto getFundDetails(
            UserEntity user,
            Long id,
            String query,
            String username,
            String reason,
            LocalDate dateFrom,
            LocalDate dateTo,
            Long amount,
            Boolean delete
    ) {
        boolean error = false;

        // Funds abrufen und optional filtern
        List<FundEntity> funds = fundRepositoryService.findFundsByUserId(user.getId());
        if (query != null) {
            funds = funds.stream()
                    .filter(f -> f.getName().toLowerCase().contains(query.toLowerCase()))
                    .toList();
        }

        List<Zahlung> filteredPayments = new ArrayList<>();
        FundEntity myFund = null;

        if (id != null) {
            FundEntity fund = fundRepositoryService.findFundById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Fund with ID " + id + " not found"));
            if (!fund.getUsers().contains(user)) {
                error = true;
            } else {
                myFund = fund;
                filteredPayments = paypalService.filterPayments(
                        fund.getPayments(), username, reason, dateFrom, dateTo, amount);
            }
        }

        // Zusammenstellen der Daten für das Frontend
        return new FundDetailsDto(funds, myFund, filteredPayments, paypalService.getCurrentBalance(), error);
    }


    public List<UserEntity> getFilteredFriends(UserEntity currentUser, String query) {
        List<UserEntity> friends = currentUser.getFriends();

        // Falls ein Query-Parameter gesetzt ist, filtere die Freunde
        if (query != null && !query.isBlank()) {
            friends = friends.stream()
                    .filter(friend -> friend.getUsername() != null &&
                            friend.getUsername().toLowerCase().contains(query.toLowerCase()))
                    .toList();
        }

        return friends;
    }


    public void processFundDeletion(Long fundId) {
        FundEntity fund = fundRepositoryService.findFundById(fundId)
                .orElseThrow(() -> new IllegalArgumentException("Fund mit ID " + fundId + " nicht gefunden"));
        fundRepositoryService.deleteFund(fund);
    }

    public void createFund(String fundname, List<Long> userIds, UserEntity loggedInUser) {
        FundEntity fund = new FundEntity();
        fund.setName(fundname);
        fund.setAdmin(loggedInUser);
        fund.getUsers().add(loggedInUser);

        if (userIds != null) {
            List<UserEntity> users = userRepositoryService.findUsersByIds(userIds);
            fund.getUsers().addAll(users);
        }

        fundRepositoryService.saveFund(fund);
    }


    public FundEntity getFund(Long fundId) {
        return fundRepositoryService.findFundById(fundId)
                .orElseThrow(() -> new IllegalArgumentException("Fund mit ID " + fundId + " nicht gefunden"));
    }

    @Transactional
    public boolean deleteFund(Long fundId, UserEntity loggedInUser, String balance) {
        FundEntity fund = fundRepositoryService.findFundById(fundId)
                .orElseThrow(() -> new IllegalArgumentException("Fund mit ID " + fundId + " nicht gefunden"));

        // Sicherstellen, dass der loggedInUser der Admin des Funds ist
        if (!isUserAdminOfFund(loggedInUser, fund)) {
            return false;
        }

        if (balance != null) {
            paymentRepositoryService.deletePaymentsByFundId(fundId);
            fundRepositoryService.deleteFund(fund);
            return true;
        }

        return false;
    }

    private boolean isUserAdminOfFund(UserEntity user, FundEntity fund) {
        return fund.getAdmin().equals(user);
    }


    @Transactional
    public void editFund(Long fundId, List<Long> userIds, String name, UserEntity loggedInUser) {
        FundEntity fund = fundRepositoryService.findFundById(fundId)
                .orElseThrow(() -> new IllegalArgumentException("Fund mit ID " + fundId + " nicht gefunden"));

        // Benutzer finden und hinzufügen
        List<UserEntity> users = userRepositoryService.findUsersByIds(userIds);
        if (!users.contains(loggedInUser)) {
            users.add(loggedInUser); // Füge den Admin hinzu
        }

        fund.setUsers(users);
        fund.setName(name);

        fundRepositoryService.saveFund(fund);
    }


    public double getLatestFundBalance(FundEntity fund) {
        return fundRepositoryService.getLatestBalanceForFund(fund);
    }

}
