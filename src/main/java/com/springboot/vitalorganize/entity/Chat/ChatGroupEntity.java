package com.springboot.vitalorganize.entity.Chat;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.springboot.vitalorganize.entity.Profile_User.UserEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Entity
@Setter
@Getter
@Table(name = "chat_group")
public class ChatGroupEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @ManyToMany
    @JoinTable(
            name = "chat_group_users", // Name der Zwischentabelle
            joinColumns = @JoinColumn(name = "chat_group_id"), // Spalte für ChatGroup-ID
            inverseJoinColumns = @JoinColumn(name = "user_id") // Spalte für User-ID
    )
    @JsonIgnore
    private List<UserEntity> users;


    @OneToMany(mappedBy = "chatGroup", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<MessageEntity> messages; // Nachrichten in der Gruppe


}
