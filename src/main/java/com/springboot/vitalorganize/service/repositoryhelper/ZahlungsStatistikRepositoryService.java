package com.springboot.vitalorganize.service.repositoryhelper;

import com.springboot.vitalorganize.model.ZahlungStatistik;
import com.springboot.vitalorganize.repository.ZahlungStatistikRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class ZahlungsStatistikRepositoryService {

    private final ZahlungStatistikRepository zahlungStatistikRepository;


    public List<ZahlungStatistik> findAll() {
        return zahlungStatistikRepository.findAll();
    }

    public ZahlungStatistik findById(Long id) {
        return zahlungStatistikRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Statistic not found with id: " + id));
    }

    public void deleteById(Long id) {
        zahlungStatistikRepository.deleteById(id);
    }

    public ZahlungStatistik saveStatistic(ZahlungStatistik zahlungStatistik) {
        return zahlungStatistikRepository.save(zahlungStatistik);
    }
}
