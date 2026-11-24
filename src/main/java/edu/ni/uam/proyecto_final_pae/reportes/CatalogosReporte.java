package edu.ni.uam.proyecto_final_pae.reportes;

import java.util.List;

public record CatalogosReporte(List<OpcionReporte> operadores, List<OpcionReporte> unidades,
                               List<OpcionReporte> clientes) {
    public CatalogosReporte {
        operadores = List.copyOf(operadores);
        unidades = List.copyOf(unidades);
        clientes = List.copyOf(clientes);
    }
}
