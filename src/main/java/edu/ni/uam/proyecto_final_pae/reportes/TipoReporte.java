package edu.ni.uam.proyecto_final_pae.reportes;

import java.util.List;

public enum TipoReporte {
    RESUMEN("Resumen financiero"), OPERADOR("Facturación por operador"),
    UNIDAD("Facturación por unidad"), EVOLUCION("Evolución de facturación"),
    DETALLE("Detalle de viajes finalizados");

    private final String titulo;

    TipoReporte(String titulo) { this.titulo = titulo; }

    public List<TipoGrafico> graficos() {
        return switch (this) {
            case OPERADOR, UNIDAD -> List.of(TipoGrafico.BARRAS, TipoGrafico.NINGUNO);
            case RESUMEN -> List.of(TipoGrafico.COLUMNAS, TipoGrafico.NINGUNO);
            case EVOLUCION -> List.of(TipoGrafico.LINEAS, TipoGrafico.COLUMNAS, TipoGrafico.NINGUNO);
            case DETALLE -> List.of(TipoGrafico.NINGUNO);
        };
    }

    public boolean temporal() { return this == RESUMEN || this == EVOLUCION; }

    @Override public String toString() { return titulo; }
}
