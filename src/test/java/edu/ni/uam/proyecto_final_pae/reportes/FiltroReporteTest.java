package edu.ni.uam.proyecto_final_pae.reportes;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FiltroReporteTest {
    private static final LocalDate DIA = LocalDate.of(2026, 11, 25);

    private FiltroReporte filtro(TipoReporte tipo, LocalDate desde, LocalDate hasta, EstadoReporte estado) {
        return new FiltroReporte(tipo, desde, hasta, null, null, null, "", null, TipoGrafico.NINGUNO, estado);
    }

    @Test void jornadasPermiteTodasLasFechasYEstadosOperativos() {
        var jornadas = filtro(TipoReporte.JORNADAS, null, null, EstadoReporte.TODOS);
        assertFalse(jornadas.tipo().financiero());
        assertFalse(jornadas.tipo().finalizados());
        assertEquals(EstadoReporte.TODOS, jornadas.estado());
        assertTrue(jornadas.descripcion().startsWith("Inicio del viaje: Todas las fechas"));
        assertDoesNotThrow(() -> filtro(TipoReporte.JORNADAS, DIA, DIA, EstadoReporte.EN_CURSO));
        assertDoesNotThrow(() -> filtro(TipoReporte.JORNADAS, DIA, DIA, EstadoReporte.PAUSADO));
    }

    @Test void cumplimientoConservaElPeriodoDeFinalizacionObligatorio() {
        var entregas = filtro(TipoReporte.CUMPLIMIENTO_ENTREGAS, DIA, DIA, null);
        assertFalse(entregas.tipo().financiero());
        assertTrue(entregas.tipo().finalizados());
        assertEquals(EstadoReporte.FINALIZADO, entregas.estado());
        assertEquals(DIA.plusDays(1).atStartOfDay(), entregas.hastaExclusivo());
        assertTrue(entregas.descripcion().startsWith("Finalización: "));
        assertThrows(IllegalArgumentException.class, () -> filtro(TipoReporte.CUMPLIMIENTO_ENTREGAS, null, null, null));
        assertThrows(IllegalArgumentException.class, () -> filtro(TipoReporte.CUMPLIMIENTO_ENTREGAS, DIA, DIA, EstadoReporte.EN_CURSO));
    }

    @Test void gestionDeViajesConservaElPeriodoOpcionalDeCreacion() {
        var gestion = filtro(TipoReporte.GESTION_VIAJES, null, null, null);
        assertFalse(gestion.tipo().financiero());
        assertFalse(gestion.tipo().finalizados());
        assertEquals(EstadoReporte.TODOS, gestion.estado());
        assertTrue(gestion.descripcion().startsWith("Creación del viaje: Todas las fechas"));
        assertThrows(IllegalArgumentException.class, () -> filtro(TipoReporte.JORNADAS, DIA, null, EstadoReporte.TODOS));
    }
}
