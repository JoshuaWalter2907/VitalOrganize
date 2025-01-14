package com.springboot.vitalorganize.service;

import com.springboot.vitalorganize.entity.Fund_Payments.FundEntity;
import com.springboot.vitalorganize.entity.Fund_Payments.PaymentEntity;
import com.springboot.vitalorganize.entity.Fund_Payments.PaymentStatisticsEntity;
import com.springboot.vitalorganize.entity.Profile_User.UserEntity;
import com.springboot.vitalorganize.model.API.StatisticsDTO;
import com.springboot.vitalorganize.repository.FundRepository;
import com.springboot.vitalorganize.repository.UserRepository;
import com.springboot.vitalorganize.repository.ZahlungStatistikRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.format.DateTimeFormatter;

import java.time.LocalDate;
import java.util.List;

/**
 * Service-Klasse für die Verwaltung von Zahlungstatistiken.
 */
@Service
@AllArgsConstructor
public class ZahlungStatistikService {

    private final FundRepository fundRepository;
    private final UserRepository userRepository;
    private ZahlungStatistikRepository zahlungStatistikRepository;

    /**
     * Ruft alle Zahlungstatistiken ab.
     * @return Eine Liste aller Zahlungstatistiken
     */
    public List<PaymentStatisticsEntity> getAllZahlungStatistiken(StatisticsDTO statisticDTO) {
        return zahlungStatistikRepository.findAllByfund(fundRepository.findById(statisticDTO.getFundId()).orElse(null));
    }

    /**
     * Ruft eine Zahlungstatistik basierend auf der ID ab.
     * @return Die Zahlungstatistik mit der angegebenen ID
     */
    public PaymentStatisticsEntity getZahlungStatistikById(StatisticsDTO statisticDTO) {
        return zahlungStatistikRepository.findById(statisticDTO.getId()).orElse(null);
    }

    /**
     * Löscht eine Zahlungstatistik basierend auf der ID.
     */
    public void deleteZahlungStatistik(StatisticsDTO statisticDTO) {
        if (zahlungStatistikRepository.findById(statisticDTO.getId()).isPresent()) {
            zahlungStatistikRepository.deleteById(statisticDTO.getId());
        }
    }

    public PaymentStatisticsEntity createZahlungStatistik(StatisticsDTO statisticDTO) {

        FundEntity fund = fundRepository.findById(statisticDTO.getFundId()).orElse(null);

        LocalDate start = LocalDate.parse(statisticDTO.getStartDate());
        LocalDate end = LocalDate.parse(statisticDTO.getEndDate());

        assert fund != null;
        List<PaymentEntity> paymentsInPeriod = fund.getPayments().stream()
                .filter(payment -> {
                    LocalDate paymentDate = payment.getDate().toLocalDate();
                    return !paymentDate.isBefore(start) && !paymentDate.isAfter(end);
                })
                .toList();

        double totalAmount = paymentsInPeriod.stream().mapToDouble(PaymentEntity::getAmount).sum();
        long paymentCount = paymentsInPeriod.size();
        double averageAmount = paymentCount > 0 ? totalAmount / paymentCount : 0.0;

        PaymentStatisticsEntity paymentStatisticsEntity = new PaymentStatisticsEntity();
        paymentStatisticsEntity.setFund(fund);
        paymentStatisticsEntity.setStartDate(start);
        paymentStatisticsEntity.setEndDate(end);
        paymentStatisticsEntity.setTotalAmount(totalAmount);
        paymentStatisticsEntity.setAverageAmount(averageAmount);
        paymentStatisticsEntity.setPaymentCount(paymentCount);

        if (!paymentsInPeriod.isEmpty()) {
            paymentStatisticsEntity.setCurrency(paymentsInPeriod.getFirst().getCurrency());
        } else {
            paymentStatisticsEntity.setCurrency("EUR");
        }

        return zahlungStatistikRepository.save(paymentStatisticsEntity);
    }

    /**
     * Ruft alle Fonds ab, die dem Benutzer zugeordnet sind, basierend auf dem Access Token.
     * @param accessToken Das Access Token des Benutzers
     * @return Eine Liste der Fonds, die mit dem Benutzer verknüpft sind
     */
    public List<FundEntity> getAllFunds(String accessToken) {
        UserEntity user = userRepository.findByToken(accessToken);
        if (user == null) {
            throw new IllegalArgumentException("Ungültiges Access Token oder Benutzer nicht gefunden.");
        }

        List<FundEntity> funds = fundRepository.findAll();
        funds = funds.stream()
                .filter(f -> f.getUsers().contains(user))  // Filtere die Fonds, die mit diesem Benutzer verknüpft sind
                .toList();

        return funds;
    }

    /**
     * Erstellt oder aktualisiert eine Zahlungstatistik, basierend auf den übermittelten Daten.
     * @return Die erstellte oder aktualisierte Zahlungstatistik
     */
    public PaymentStatisticsEntity createOrUpdateZahlungStatistik(StatisticsDTO statisticDTO) {

        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;
        LocalDate startDate = LocalDate.parse(statisticDTO.getStartDate(), formatter);
        LocalDate endDate = LocalDate.parse(statisticDTO.getEndDate(), formatter);

        PaymentStatisticsEntity existing = zahlungStatistikRepository.findById(statisticDTO.getId()).orElse(null);

        if (existing != null) {
            FundEntity fund = fundRepository.findById(statisticDTO.getFundId()).orElse(null);
            assert fund != null;
            List<PaymentEntity> paymentsInPeriod = fund.getPayments().stream()
                    .filter(payment -> {
                        LocalDate paymentDate = payment.getDate().toLocalDate();
                        return !paymentDate.isBefore(startDate) && !paymentDate.isAfter(endDate);
                    })
                    .toList();

            double totalAmount = paymentsInPeriod.stream().mapToDouble(PaymentEntity::getAmount).sum();
            long paymentCount = paymentsInPeriod.size();
            double averageAmount = paymentCount > 0 ? totalAmount / paymentCount : 0.0;

            existing.setFund(fundRepository.findById(statisticDTO.getFundId()).orElse(null));
            existing.setStartDate(startDate);
            existing.setEndDate(endDate);
            existing.setTotalAmount(totalAmount);
            existing.setAverageAmount(averageAmount);
            existing.setPaymentCount(paymentCount);
            return zahlungStatistikRepository.save(existing);
        } else {
            return createZahlungStatistik(statisticDTO);
        }
    }

    /**
     * Extrahiert das Access Token aus dem Authorization-Header.
     * @param authorizationHeader Der Authorization-Header im Format "Bearer <token>"
     * @return Das Access Token
     * @throws IllegalArgumentException Wenn der Header nicht im erwarteten Format ist
     */
    public String extractAccessToken(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        } else {
            throw new IllegalArgumentException("Der Authorization-Header muss im Format 'Bearer <token>' sein.");
        }
    }
}