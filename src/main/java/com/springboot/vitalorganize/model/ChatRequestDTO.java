package com.springboot.vitalorganize.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatRequestDTO {

    private Long user2;
    private Long group;
    private String query;

}
