package com.springboot.vitalorganize.entity.Chat;

import com.springboot.vitalorganize.entity.Profile_User.UserEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Setter
@Getter
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
    private ChatGroupEntity chatGroup;

    @ManyToOne
    @JoinColumn(name = "direct_chat_id", nullable = true)  // Verweis auf die DirectChat-Tabelle
    private DirectChatEntity directChat;  // Verweis auf das DirectChat

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
