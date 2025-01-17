package com.springboot.vitalorganize.model.API;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StatisticsDTO {

    private Long id;
    private Long fundId;
    private String startDate;
    private String endDate;

}
