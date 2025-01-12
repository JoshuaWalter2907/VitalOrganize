package com.springboot.vitalorganize.service.repositoryhelper;

import com.springboot.vitalorganize.entity.FundEntity;
import com.springboot.vitalorganize.entity.UserEntity;
import com.springboot.vitalorganize.repository.FundRepository;
import com.springboot.vitalorganize.repository.PaymentRepository;
import com.springboot.vitalorganize.entity.Payment;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service-Klasse zur Verwaltung von Fonds und zugehörigen Zahlungen.
 * Bietet Methoden zum Abrufen, Speichern und Löschen von Fonds und Zahlungen im Repository.
 */
@Service
@AllArgsConstructor
public class FundRepositoryService {

    private final FundRepository fundRepository;
    private final PaymentRepository paymentRepository;

    /**
     * Sucht einen Fonds anhand seiner ID.
     * Wenn der Fonds nicht gefunden wird, wird eine IllegalArgumentException geworfen.
     *
     * @param fundId die ID des gesuchten Fonds
     * @return der gefundene Fonds
     * @throws IllegalArgumentException wenn der Fonds nicht gefunden wird
     */
    public FundEntity findFundById(Long fundId) {
        return fundRepository.findById(fundId)
                .orElseThrow(() -> new IllegalArgumentException("Fund with ID " + fundId + " not found"));
    }

    /**
     * Sucht alle Fonds eines bestimmten Benutzers anhand seiner ID.
     *
     * @param userId die ID des Benutzers
     * @return eine Liste der Fonds des Benutzers
     */
    public List<FundEntity> findFundsByUserId(Long userId) {
        return fundRepository.findFundsByUserId(userId);
    }

    /**
     * Speichert einen Fonds im Repository.
     *
     * @param fund der zu speichernde Fonds
     */
    public void saveFund(FundEntity fund) {
        fundRepository.save(fund);
    }

    /**
     * Löscht einen Fonds und alle zugehörigen Zahlungen aus dem Repository.
     *
     * @param fund der zu löschende Fonds
     */
    public void deleteFund(FundEntity fund) {
        paymentRepository.deleteByFundId(fund.getId()); // Löscht alle Zahlungen, die mit diesem Fonds verknüpft sind
        fundRepository.delete(fund); // Löscht den Fonds
    }

    /**
     * Löscht alle Zahlungen, die mit einem bestimmten Fonds verknüpft sind.
     *
     * @param fundId die ID des Fonds, dessen Zahlungen gelöscht werden sollen
     */
    public void deletePaymentsByFundId(Long fundId) {
        paymentRepository.deleteByFundId(fundId);
    }

    /**
     * Sucht alle Zahlungen, die mit einem bestimmten Fonds verknüpft sind.
     *
     * @param fundId die ID des Fonds, dessen Zahlungen gesucht werden sollen
     * @return eine Liste der Zahlungen des Fonds
     */
    public List<Payment> findPaymentsByFundId(Long fundId) {
        FundEntity fund = fundRepository.findById(fundId)
                .orElseThrow(() -> new IllegalArgumentException("Fund mit ID " + fundId + " nicht gefunden"));
        return fund.getPayments(); // Gibt alle Zahlungen des Fonds zurück
    }

    /**
     * Berechnet den aktuellen Saldo eines Fonds basierend auf den letzten Zahlungen.
     * Wenn keine Zahlungen vorhanden sind, wird ein Saldo von 0.0 zurückgegeben.
     *
     * @param fund der Fonds, dessen aktueller Saldo berechnet werden soll
     * @return der aktuelle Saldo des Fonds
     */
    public double getLatestBalanceForFund(FundEntity fund) {
        if (fund.getPayments().isEmpty()) {
            return 0.0; // Wenn keine Zahlungen vorhanden sind, wird der Saldo als 0.0 angenommen
        }
        return fund.getPayments().getLast().getBalance(); // Gibt den Saldo der letzten Zahlung zurück
    }

    /**
     * Sucht alle Fonds, die von einem bestimmten Benutzer als Administrator verwaltet werden.
     *
     * @param user der Benutzer, dessen verwaltete Fonds gesucht werden
     * @return eine Liste der Fonds, die der Benutzer verwaltet
     */
    public List<FundEntity> findByAdmin(UserEntity user) {
        return fundRepository.findByAdmin(user);
    }

    /**
     * Gibt alle Fonds im Repository zurück.
     *
     * @return eine Liste aller Fonds
     */
    public List<FundEntity> findALl() {
        return fundRepository.findAll();
    }
}
