package com.springboot.vitalorganize.dto;

import com.springboot.vitalorganize.model.FundEntity;
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
