package com.springboot.vitalorganize.model.Fund_Payment;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentInformationRequestDTO {

    private String amount;
    private String type;
    private String description;
    private String email;
    private Long fundid;

}
