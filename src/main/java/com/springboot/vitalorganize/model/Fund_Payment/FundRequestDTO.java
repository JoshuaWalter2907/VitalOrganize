package com.springboot.vitalorganize.model.Fund_Payment;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class FundRequestDTO {

    private Long id;
    private String query;
    private boolean show;
    private String username;
    private String reason;
    private LocalDate datefrom;
    private LocalDate dateto;
    private Long amount;
    int page = 0;
    int size = 3;

}
