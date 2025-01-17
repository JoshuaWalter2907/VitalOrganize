package com.springboot.vitalorganize.model.Fund_Payment;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class EditFundRequestDTO {

    private Long fundId;
    private List<Long> selectedUsers;
    private String fundname;

}
