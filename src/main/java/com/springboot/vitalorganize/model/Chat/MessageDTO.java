package com.springboot.vitalorganize.model.Chat;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class MessageDTO {
    private String content;
    private Long senderId;
    private Long recipientId;
    private Long chatGroupId;

    // Getter und Setter

}
