package com.springboot.vitalorganize.model.Chat;

import com.springboot.vitalorganize.entity.Profile_User.UserEntity;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class NewChatResponseDTO {

    private Map<Character, List<UserEntity>> groupedUsers;
    private String currentUrl;
}
