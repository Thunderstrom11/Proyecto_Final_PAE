package edu.ni.uam.proyecto_final_pae.reportes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Proyección inmutable: no conserva entidades ni relaciones Hibernate. */
public record ViajeReporte(Integer id, String carga, Integer clienteId, String cliente,
                           Integer operadorId, String operador, Integer unidadId, String unidad,
                           LocalDateTime inicio, LocalDateTime fin, BigDecimal importe) { }
