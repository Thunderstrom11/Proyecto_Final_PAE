package com.fleetroute.api.dto;

import java.time.OffsetDateTime;

public record LoginResponse(
        String token,
        OffsetDateTime expira,
        OperadorInfo operador,
        EmpresaInfo empresa) {
}
