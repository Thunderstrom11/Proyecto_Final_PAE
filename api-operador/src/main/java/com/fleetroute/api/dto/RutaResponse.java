package com.fleetroute.api.dto;

import java.util.List;

public record RutaResponse(
        String proveedor,
        double distanciaMetros,
        double duracionSegundos,
        List<Coordenada> puntos,
        List<Paso> pasos) {

    public record Coordenada(double lat, double lng) {
    }

    public record Paso(
            int indice,
            int inicio,
            int fin,
            double distanciaMetros,
            double duracionSegundos,
            String instruccion,
            String nombre,
            int tipo) {
    }
}
