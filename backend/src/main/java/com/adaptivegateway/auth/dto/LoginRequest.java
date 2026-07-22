package com.adaptivegateway.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank @Size(min = 3, max = 254) String usernameOrEmail,
        @NotBlank @Size(min = 12, max = 128) String password
) {
}
