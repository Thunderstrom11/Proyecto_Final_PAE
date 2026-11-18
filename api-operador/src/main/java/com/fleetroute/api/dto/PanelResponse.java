package com.fleetroute.api.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PanelResponse(
        OperadorInfo operador,
        EmpresaInfo empresa,
        OffsetDateTime actualizado,
        boolean tieneViaje,
        Viaje viaje,
        Jornada jornada,
        Siguiente siguiente) {

    public record Viaje(
            int id,
            int idUnidad,
            String estado,
            String numeroCarga,
            String descripcionCarga,
            BigDecimal pesoToneladas,
            String numeroUnidad,
            String placas,
            String modeloUnidad,
            String origen,
            String departamentoOrigen,
            String paisOrigen,
            Double latitudOrigen,
            Double longitudOrigen,
            String destino,
            String departamentoDestino,
            String paisDestino,
            Double latitudDestino,
            Double longitudDestino,
            OffsetDateTime fechaLimite,
            BigDecimal tiempoEstimado,
            BigDecimal cobroViaje,
            BigDecimal horasMaxConduccion,
            BigDecimal horasMinDescanso,
            OffsetDateTime fechaInicio) {
    }

    public record Jornada(
            String paso,
            String pasoEtiqueta,
            String semaforo,
            String semaforoEtiqueta,
            String semaforoColor,
            String mensaje,
            int porcentajeManejo,
            long manejoMinutos,
            long descansoMinutos,
            long descansoRestanteMinutos,
            String manejoTexto,
            String descansoTexto,
            String descansoRestanteTexto,
            String descansoMinimoTexto,
            OffsetDateTime llegadaProyectada,
            boolean puedeReanudar) {
    }

    public record Siguiente(
            int id,
            String estado,
            String numeroCarga,
            String origen,
            String destino,
            String numeroUnidad,
            OffsetDateTime fechaLimite,
            BigDecimal tiempoEstimado) {
    }
}
