package com.springboot.vitalorganize.model.Chat;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreateChatGroupRequestDTO {
    private List<Long> selectedUsers;
    private String chatName;
    private String currentUrl;
}
