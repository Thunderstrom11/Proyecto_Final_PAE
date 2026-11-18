package com.fleetroute.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CambioPasswordRequest(
        @NotBlank(message = "Escriba la contraseña actual.") String actual,
        @NotBlank(message = "Escriba la nueva contraseña.")
        @Size(min = 8, message = "La nueva contraseña debe tener al menos 8 caracteres.") String nueva) {
}
