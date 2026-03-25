package com.adventurebook.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlayerRegistrationRequest {

    @NotBlank(message = "Player name must not be blank")
    @Size(min = 1, max = 100, message = "Player name must be between 1 and 100 characters")
    private String name;
}
