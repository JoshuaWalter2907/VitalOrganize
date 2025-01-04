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

/**
 * Service zur Verwaltung von Fonds, deren Benutzern und Zahlungen.
 * Bietet Methoden zum Abrufen von Fondsdaten, Erstellen, Bearbeiten, Löschen und Berechnen von Fonds.
 */
@Service
@AllArgsConstructor
public class FundService {

    private final FundRepositoryService fundRepositoryService;
    private final UserRepositoryService userRepositoryService;
    private final PaypalService paypalService;

    /**
     * Holt die Details eines Fonds einschließlich Zahlungen und Benutzer.
     * Es können optionale Filter angewendet werden (z.B. nach Name des Fonds, Benutzername, Zahlungen).
     *
     * @param user der aktuell angemeldete Benutzer
     * @param id ID des Fonds, dessen Details abgerufen werden sollen
     * @param query optionaler Filter für den Fondsnamen
     * @param username optionaler Filter für den Benutzernamen in Zahlungen
     * @param reason optionaler Filter für den Grund in Zahlungen
     * @param dateFrom optionaler Startdatum-Filter für Zahlungen
     * @param dateTo optionaler Enddatum-Filter für Zahlungen
     * @param amount optionaler Filter für den Betrag der Zahlungen
     * @return ein DTO mit den angeforderten Fondsdaten
     */
    public FundDetailsDto getFundDetails(
            UserEntity user,
            Long id,
            String query,
            String username,
            String reason,
            LocalDate dateFrom,
            LocalDate dateTo,
            Long amount
    ) {
        boolean error = false;

        // Fonds abrufen und optional filtern
        List<FundEntity> funds = fundRepositoryService.findFundsByUserId(user.getId());
        if (query != null) {
            funds = funds.stream()
                    .filter(f -> f.getName().toLowerCase().contains(query.toLowerCase()))
                    .toList();
        }

        List<Payment> filteredPayments = new ArrayList<>();
        FundEntity myFund = null;

        if (id != null) {
            FundEntity fund = fundRepositoryService.findFundById(id);
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

    /**
     * Filtert die Freunde des aktuellen Benutzers basierend auf einem optionalen Suchbegriff.
     *
     * @param currentUser der aktuell angemeldete Benutzer
     * @param query optionaler Suchbegriff für die Benutzernamen der Freunde
     * @return eine Liste der gefilterten Freunde
     */
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

    /**
     * Löscht einen Fund aus der Datenbank.
     *
     * @param fundId die ID des zu löschenden Fonds
     */
    public void processFundDeletion(Long fundId) {
        FundEntity fund = fundRepositoryService.findFundById(fundId);
        fundRepositoryService.deleteFund(fund);
    }

    /**
     * Erstellt einen neuen Fund und fügt Benutzer hinzu.
     *
     * @param fundname der Name des Fonds
     * @param userIds eine Liste von Benutzer-IDs, die dem Fund hinzugefügt werden sollen
     * @param loggedInUser der aktuell angemeldete Benutzer, der den Fund erstellt
     */
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

    /**
     * Holt einen Fund anhand seiner ID.
     *
     * @param fundId die ID des Fonds
     * @return der Fund mit der angegebenen ID
     */
    public FundEntity getFund(Long fundId) {
        return fundRepositoryService.findFundById(fundId);
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

    /**
     * Bearbeitet einen Fund, fügt Benutzer hinzu und aktualisiert den Fundnamen.
     * Dieser Vorgang wird in einer Transaktion ausgeführt.
     *
     * @param fundId die ID des Fonds, der bearbeitet werden soll
     * @param userIds eine Liste von Benutzer-IDs, die dem Fund hinzugefügt werden sollen
     * @param name der neue Name des Fonds
     * @param loggedInUser der aktuell angemeldete Benutzer, der die Änderungen vornimmt
     */
    @Transactional
    public void editFund(Long fundId, List<Long> userIds, String name, UserEntity loggedInUser) {
        FundEntity fund = fundRepositoryService.findFundById(fundId);

        // Benutzer finden und hinzufügen
        List<UserEntity> users = userRepositoryService.findUsersByIds(userIds);
        if (!users.contains(loggedInUser)) {
            users.add(loggedInUser); // Füge den Admin hinzu
        }

        if(name != null && !name.isBlank()) {
            fund.setName(name);
        }
        fund.setUsers(users);

        fundRepositoryService.saveFund(fund);
    }

    /**
     * Holt den aktuellen Kontostand eines Fonds.
     *
     * @param fund der Fund, für den der Kontostand abgerufen werden soll
     * @return der aktuelle Kontostand des Fonds
     */
    public double getLatestFundBalance(FundEntity fund) {
        return fundRepositoryService.getLatestBalanceForFund(fund);
    }

    /**
     * Löscht einen Fund aus der Datenbank. Nur der Administrator des Fonds kann diesen Vorgang durchführen.
     * Dieser Vorgang wird in einer Transaktion ausgeführt.
     *
     * @param fundId die ID des Fonds, der gelöscht werden soll
     * @param loggedInUser der aktuell angemeldete Benutzer, der den Fund löschen möchte
     * @param balance der aktuelle Kontostand des Fonds
     * @return true, wenn der Fund erfolgreich gelöscht wurde
     * @throws SecurityException wenn der Benutzer nicht der Administrator des Fonds ist
     */
    @Transactional
    public boolean deleteFund(Long fundId, UserEntity loggedInUser, String balance) {
        FundEntity fund = fundRepositoryService.findFundById(fundId);

        if (!isUserAdminOfFund(loggedInUser, fund)) {
            throw new SecurityException("Nicht autorisiert, um den Fund zu löschen.");
        }

        fundRepositoryService.deleteFund(fund);
        return true;
    }
}
