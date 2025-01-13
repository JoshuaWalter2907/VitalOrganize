package com.springboot.vitalorganize.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class Verify2FARequestDTO {

    private Map<String, String> digits;

}
