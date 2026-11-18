package com.fleetroute.api.dto;

public record AlertaRequest(
        Double latitud,
        Double longitud,
        String ubicacion,
        String mensaje) {
}
