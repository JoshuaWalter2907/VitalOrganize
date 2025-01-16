package com.springboot.vitalorganize.entity.Fund_Payments;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.springboot.vitalorganize.entity.Profile_User.UserEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Table(name = "fund")
@Entity
@Getter
@Setter
public class FundEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @ManyToMany
    @JsonIgnore
    @JoinTable(
            name = "fund_users",
            joinColumns = @JoinColumn(name = "fund_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private List<UserEntity> users = new ArrayList<>();

    @ManyToOne
    @JsonIgnore
    @JoinColumn(name = "admin_id")
    private UserEntity admin;

    @OneToMany(mappedBy = "fund",cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PaymentEntity> payments;
}
