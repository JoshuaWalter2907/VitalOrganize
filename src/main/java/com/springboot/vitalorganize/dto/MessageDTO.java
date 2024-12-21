package com.springboot.vitalorganize.dto;

import com.springboot.vitalorganize.model.UserEntity;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.userdetails.User;

@Setter
@Getter
public class MessageDTO {
    private String content;
    private Long senderId;
    private Long recipientId;
    private Long chatGroupId;

    // Getter und Setter

}
