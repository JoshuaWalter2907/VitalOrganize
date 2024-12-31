package com.springboot.vitalorganize.repository;

import com.springboot.vitalorganize.model.FundEntity;
import com.springboot.vitalorganize.model.ZahlungStatistik;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface ZahlungStatistikRepository extends JpaRepository<ZahlungStatistik, Long> {

    List<ZahlungStatistik> findAllByfund(FundEntity fund);
}
