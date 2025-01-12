package com.springboot.vitalorganize.model;

import com.springboot.vitalorganize.entity.FundEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ZahlungStatistikRequest {
    private FundEntity fundId;
    private String startDate;
    private String endDate;

    // Getter und Setter
}
