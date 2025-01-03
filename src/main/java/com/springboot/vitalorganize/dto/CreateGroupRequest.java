package com.springboot.vitalorganize.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreateGroupRequest {
    private List<Long> selectedUsers;
    private String chatName;
    private String currentUrl;
}
