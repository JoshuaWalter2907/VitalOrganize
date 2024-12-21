package com.springboot.vitalorganize.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Setter
@Getter
@Table(name = "direct_chat")
public class DirectChat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user1_id", nullable = false)
    private UserEntity user1;

    @ManyToOne
    @JoinColumn(name = "user2_id", nullable = false)
    private UserEntity user2;

    @OneToMany(mappedBy = "directChat", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<MessageEntity> messages;
}
