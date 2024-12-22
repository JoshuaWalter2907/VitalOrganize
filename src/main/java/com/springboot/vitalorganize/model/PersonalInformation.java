package com.springboot.vitalorganize.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;


@Setter
@Getter
@Entity
@Table(name = "personal_information")
public class PersonalInformation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String firstName;
    private String lastName;
    private String address;
    private String postalCode;
    private String city;
    private String region;
    private String country;

    @OneToOne
    @JoinColumn(name = "user_id")
    private UserEntity user;  // Verknüpfung zur UserEntity
}
