package com.fleetroute.api.dto;

import java.time.OffsetDateTime;

public record AlertaResponse(
        int id,
        OffsetDateTime fechaHora,
        String ubicacion,
        String mensaje) {
}
