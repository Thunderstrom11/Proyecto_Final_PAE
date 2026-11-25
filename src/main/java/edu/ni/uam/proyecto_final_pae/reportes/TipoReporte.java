package edu.ni.uam.proyecto_final_pae.reportes;

import java.util.List;

public enum TipoReporte {
    RESUMEN("Resumen financiero"), OPERADOR("Facturación por operador"),
    UNIDAD("Facturación por unidad"), EVOLUCION("Evolución de facturación"),
    DETALLE("Detalle de viajes finalizados"), GESTION_VIAJES("Gestión de viajes"),
    JORNADAS("Jornadas y descanso de operadores");

    private final String titulo;

    TipoReporte(String titulo) { this.titulo = titulo; }

    public List<TipoGrafico> graficos() {
        return switch (this) {
            case OPERADOR, UNIDAD -> List.of(TipoGrafico.BARRAS, TipoGrafico.NINGUNO);
            case RESUMEN -> List.of(TipoGrafico.COLUMNAS, TipoGrafico.NINGUNO);
            case EVOLUCION -> List.of(TipoGrafico.LINEAS, TipoGrafico.COLUMNAS, TipoGrafico.NINGUNO);
            case DETALLE, GESTION_VIAJES, JORNADAS -> List.of(TipoGrafico.NINGUNO);
        };
    }

    public boolean temporal() { return this == RESUMEN || this == EVOLUCION; }
    public boolean financiero() { return this != GESTION_VIAJES && this != JORNADAS; }
    public boolean detallado() { return this == DETALLE || this == GESTION_VIAJES || this == JORNADAS; }

    /** Fecha del viaje sobre la que se aplica el período. */
    public String etiquetaFecha() {
        return switch (this) {
            case GESTION_VIAJES -> "Creación del viaje";
            case JORNADAS -> "Inicio del viaje";
            default -> "Finalización";
        };
    }

    @Override public String toString() { return titulo; }
}
