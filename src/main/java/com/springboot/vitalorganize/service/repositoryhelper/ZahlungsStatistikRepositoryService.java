package com.springboot.vitalorganize.service.repositoryhelper;

import com.springboot.vitalorganize.entity.FundEntity;
import com.springboot.vitalorganize.entity.ZahlungStatistik;
import com.springboot.vitalorganize.repository.ZahlungStatistikRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service-Klasse zur Verwaltung von Zahlungstatistiken.
 * Diese Klasse bietet Methoden zum Abrufen, Speichern und Löschen von Zahlungstatistiken im Repository.
 */
@Service
@AllArgsConstructor
public class ZahlungsStatistikRepositoryService {

    private final ZahlungStatistikRepository zahlungStatistikRepository;

    /**
     * Gibt alle Zahlungstatistiken aus dem Repository zurück.
     *
     * @return eine Liste von Zahlungstatistiken
     */
    public List<ZahlungStatistik> findAll() {
        // Gibt alle Zahlungstatistiken im Repository zurück
        return zahlungStatistikRepository.findAll();
    }

    /**
     * Findet eine Zahlungstatistik anhand ihrer ID.
     *
     * @param id die ID der Zahlungstatistik
     * @return die gefundene Zahlungstatistik
     * @throws IllegalArgumentException wenn die Zahlungstatistik mit der angegebenen ID nicht gefunden wird
     */
    public ZahlungStatistik findById(Long id) {
        // Sucht nach der Zahlungstatistik mit der angegebenen ID und wirft eine Ausnahme, wenn diese nicht existiert
        return zahlungStatistikRepository.findById(id)
                .orElse(null);
    }

    /**
     * Löscht eine Zahlungstatistik anhand ihrer ID.
     *
     * @param id die ID der zu löschenden Zahlungstatistik
     */
    public void deleteById(Long id) {
        // Löscht die Zahlungstatistik mit der angegebenen ID aus dem Repository
        zahlungStatistikRepository.deleteById(id);
    }

    /**
     * Speichert eine Zahlungstatistik im Repository.
     *
     * @param zahlungStatistik die zu speichernde Zahlungstatistik
     * @return die gespeicherte Zahlungstatistik
     */
    public ZahlungStatistik saveStatistic(ZahlungStatistik zahlungStatistik) {
        // Speichert die übergebene Zahlungstatistik im Repository
        return zahlungStatistikRepository.save(zahlungStatistik);
    }

    public List<ZahlungStatistik> findAllByFundId(FundEntity fundEntity) {
        return zahlungStatistikRepository.findAllByfund(fundEntity);
    }
}
