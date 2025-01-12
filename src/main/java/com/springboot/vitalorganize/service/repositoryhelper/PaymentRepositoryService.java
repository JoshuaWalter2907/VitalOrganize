package com.springboot.vitalorganize.service.repositoryhelper;

import com.springboot.vitalorganize.entity.UserEntity;
import com.springboot.vitalorganize.entity.Payment;
import com.springboot.vitalorganize.repository.PaymentRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service-Klasse zur Verwaltung von Zahlungen.
 * Diese Klasse bietet Methoden zum Abrufen, Speichern und Bearbeiten von Zahlungen im Repository.
 * Sie stellt sicher, dass die Zahlungen korrekt verarbeitet werden und ermöglicht den Zugriff auf Zahlungen nach Benutzer oder Fonds.
 */
@Service
@AllArgsConstructor
public class PaymentRepositoryService {

    private final PaymentRepository paymentRepository;

    /**
     * Sucht alle Zahlungen, die mit einem bestimmten Benutzer verknüpft sind.
     *
     * @param user der Benutzer, dessen Zahlungen abgerufen werden sollen
     * @return eine Liste von Zahlungen, die dem Benutzer zugeordnet sind
     */
    public List<Payment> findPaymentsByUser(UserEntity user) {
        // Abrufen aller Zahlungen, die dem angegebenen Benutzer zugeordnet sind
        return paymentRepository.findAllByUser(user); // Angenommen, PaymentRepository hat diese Methode
    }

    /**
     * Sucht die letzte Transaktion für einen bestimmten Fonds anhand der Fonds-ID.
     *
     * @param id die ID des Fonds, für den die letzte Transaktion gesucht wird
     * @return die letzte Zahlungstransaktion für den angegebenen Fonds
     */
    public Payment findLatestTransactionByFundId(Long id) {
        // Abrufen der letzten Zahlungstransaktion für den angegebenen Fonds
        return paymentRepository.findLatestTransactionByFundId(id);
    }

    /**
     * Speichert eine Zahlung im Repository.
     *
     * @param payment die zu speichernde Zahlung
     */
    public void savePayment(Payment payment) {
        // Speichern der Zahlung im Repository
        paymentRepository.save(payment);
    }

    /**
     * Sucht die letzte Zahlungstransaktion im System, unabhängig vom Fonds.
     *
     * @return die letzte Zahlungstransaktion
     */
    public Payment findLatestTransaction() {
        // Abrufen der letzten Zahlungstransaktion aus dem Repository
        return paymentRepository.findLatestTransaction();
    }

    /**
     * Setzt alle Benutzerverweise in den Zahlungen auf null für eine bestimmte Benutzer-ID.
     * Dies kann nützlich sein, wenn ein Benutzer gelöscht oder entfernt wird und alle zugehörigen Zahlungen angepasst werden müssen.
     *
     * @param id die Benutzer-ID, für die alle Verweise auf null gesetzt werden sollen
     */
    public void updateUserReferencesToNull(Long id) {
        // Aktualisieren der Zahlungen, um alle Benutzerverweise auf null zu setzen
        paymentRepository.updateUserReferencesToNull(id);
    }
}
