package com.springboot.vitalorganize.model.Chat;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatRequestDTO {

    private Long user2;
    private Long group;
    private String query;
    private int size = 20;
    private int page = 0;

}
