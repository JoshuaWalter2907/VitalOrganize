package com.springboot.vitalorganize.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FARequestDTO {

    private String email;
    private String inputString;
    private String birthDate;
    private String isPublic;

}
