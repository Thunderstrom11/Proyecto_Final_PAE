package com.fleetroute.api.security;

import java.time.Instant;

public record Sesion(
        String token,
        int idUsuario,
        int idOperador,
        String username,
        String nombreOperador,
        String cedula,
        String empresa,
        String zonaHoraria,
        String moneda,
        Instant expira) {
}
