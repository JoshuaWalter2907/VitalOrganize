package com.springboot.vitalorganize.model;

import com.springboot.vitalorganize.entity.UserEntity;
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
