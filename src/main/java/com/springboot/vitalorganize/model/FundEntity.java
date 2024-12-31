package com.springboot.vitalorganize.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
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
    @JoinTable(
            name = "fund_users",
            joinColumns = @JoinColumn(name = "fund_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private List<UserEntity> users = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "admin_id")
    private UserEntity admin;

    @OneToMany(mappedBy = "fund",cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Zahlung> payments;
}
