package com.springboot.vitalorganize.model.Profile;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class Verify2FARequestDTO {

    private List<String> digits;

}
