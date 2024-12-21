package com.springboot.vitalorganize.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ChatGroup {

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
