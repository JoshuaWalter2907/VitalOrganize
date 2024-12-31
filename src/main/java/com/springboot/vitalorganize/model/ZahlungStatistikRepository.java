package com.springboot.vitalorganize.model;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface ZahlungStatistikRepository extends JpaRepository<ZahlungStatistik, Long> {

    List<ZahlungStatistik> findAllByfund(FundEntity fund);
}
