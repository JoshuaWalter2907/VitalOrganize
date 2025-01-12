package com.springboot.vitalorganize.repository;

import com.springboot.vitalorganize.entity.FundEntity;
import com.springboot.vitalorganize.entity.ZahlungStatistik;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface ZahlungStatistikRepository extends JpaRepository<ZahlungStatistik, Long> {

    List<ZahlungStatistik> findAllByfund(FundEntity fund);
}
