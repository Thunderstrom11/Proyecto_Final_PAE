package edu.ni.uam.proyecto_final_pae.reportes;

import java.util.List;

public enum EstadoReporte {
    TODOS("Todos", List.of()), ACTIVOS("Activos", List.of("PROGRAMADO", "EN_CURSO", "PAUSADO")),
    PROGRAMADO("Programado", List.of("PROGRAMADO")), EN_CURSO("En curso", List.of("EN_CURSO")),
    PAUSADO("Pausado", List.of("PAUSADO")), FINALIZADO("Finalizado", List.of("FINALIZADO")),
    CANCELADO("Cancelado", List.of("CANCELADO"));
    private final String nombre;
    private final List<String> estados;
    EstadoReporte(String nombre, List<String> estados) { this.nombre = nombre; this.estados = estados; }
    public List<String> estados() { return estados; }
    @Override public String toString() { return nombre; }
}
