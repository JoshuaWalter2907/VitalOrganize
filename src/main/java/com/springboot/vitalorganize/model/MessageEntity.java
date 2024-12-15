package com.springboot.vitalorganize.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "message_entity")
public class MessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "sender_id", nullable = false)
    private UserEntity sender;

    @ManyToOne
    @JoinColumn(name = "recipient_id", nullable = true)
    private UserEntity recipient;

    @ManyToOne
    @JoinColumn(name = "chat_group_id", nullable = true) // Gruppe, in der die Nachricht gesendet wurde
    private ChatGroup chatGroup;

    @ManyToOne
    @JoinColumn(name = "direct_chat_id", nullable = true)  // Verweis auf die DirectChat-Tabelle
    private DirectChat directChat;  // Verweis auf das DirectChat

    @Column(nullable = false)
        private String content;

    @Column(nullable = false)
        private LocalDateTime timestamp;



    @Override
    public String toString() {
        return "MessageEntity{" +
                "id=" + id +
                ", sender=" + (sender != null ? sender.getUsername() : "null") +
                ", recipient=" + (recipient != null ? recipient.getUsername() : "null") +
                ", content='" + content + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
