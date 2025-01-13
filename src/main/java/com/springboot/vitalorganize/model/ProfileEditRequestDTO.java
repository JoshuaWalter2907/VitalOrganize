package com.springboot.vitalorganize.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class ProfileEditRequestDTO {
    private Long profileId;
    private boolean auth;
    private boolean fa;
    private String tab;
    private String kind;
    private String username;
    private String reason;
    private LocalDate datefrom;
    private LocalDate dateto;
    private Long amount;
    private String address;
    private String city;
    private String region;
    private String postalCode;
    private String surname;
    private String name;
    private String publicPrivateToggle;

    // Getter und Setter
}
