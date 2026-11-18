package com.fleetroute.api.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Escriba el usuario.") String usuario,
        @NotBlank(message = "Escriba la contraseña.") String password) {
}
