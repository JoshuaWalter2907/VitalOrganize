package com.springboot.vitalorganize.model.Fund_Payment;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentInformationSessionDTO {

    private String amount;
    private String type;
    private String description;
    private String receiverEmail;
    private Long fundid;
    private Long id;

}
