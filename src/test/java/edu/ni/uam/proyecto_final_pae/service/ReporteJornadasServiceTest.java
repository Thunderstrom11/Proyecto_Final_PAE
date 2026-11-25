package edu.ni.uam.proyecto_final_pae.service;

import edu.ni.uam.proyecto_final_pae.reportes.ViajeJornada;
import edu.ni.uam.proyecto_final_pae.reportes.ViajeJornada.Evento;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import net.sf.jasperreports.engine.JasperExportManager;
import org.junit.jupiter.api.Test;
import static edu.ni.uam.proyecto_final_pae.service.JornadaService.*;
import static org.junit.jupiter.api.Assertions.*;

class ReporteJornadasServiceTest {
    private static final LocalDateTime T0 = LocalDateTime.of(2026, 11, 20, 6, 0);

    private static ViajeJornada viaje(int id, int operador, String nombre, String estado, Evento... eventos) {
        return new ViajeJornada(id, "C-" + id, operador, nombre, "U-1", estado, T0,
                new BigDecimal("8"), new BigDecimal("2"), List.of(eventos));
    }

    private static Evento ev(String tipo, double horas) {
        return new Evento(tipo, T0.plusMinutes(Math.round(horas * 60)));
    }

    @Test void viajeDentroDelLimiteEsConforme() {
        var e = ReporteJornadasService.evaluar(viaje(1, 1, "Ana", "FINALIZADO",
                ev(INICIO_TRAYECTO, 0), ev(CONFIRMAR_ENTREGA, 6)), T0.plusDays(1));
        assertEquals(Duration.ofHours(6), e.manejo());
        assertFalse(e.incumple());
        assertEquals("Conforme", e.resultado());
        assertFalse(e.abierto());
    }

    @Test void tramoContinuoMayorAlLimiteEsExceso() {
        var e = ReporteJornadasService.evaluar(viaje(2, 1, "Ana", "FINALIZADO",
                ev(INICIO_TRAYECTO, 0), ev(CONFIRMAR_ENTREGA, 9)), T0.plusDays(1));
        assertTrue(e.excesoManejo());
        assertEquals(Duration.ofHours(9), e.tramoMaximo());
    }

    @Test void pausaAutomaticaDentroDeLaToleranciaNoEsExceso() {
        var e = ReporteJornadasService.evaluar(viaje(3, 1, "Ana", "FINALIZADO",
                ev(INICIO_TRAYECTO, 0), ev(PAUSA_DESCANSO, 8.05), ev(REANUDAR, 10.1), ev(CONFIRMAR_ENTREGA, 12)), T0.plusDays(1));
        assertFalse(e.excesoManejo());
        assertFalse(e.descansoInsuficiente(), "Descansó más de 2 h antes de seguir manejando");
        assertEquals(1, e.pausas());
        assertEquals(Duration.ofHours(2), e.descansoRequerido());
    }

    @Test void descansoFragmentadoSeAcumula() {
        var e = ReporteJornadasService.evaluar(viaje(4, 1, "Ana", "FINALIZADO",
                ev(INICIO_TRAYECTO, 0), ev(PAUSA_DESCANSO, 5), ev(REANUDAR, 6), ev(PAUSA_DESCANSO, 9),
                ev(REANUDAR, 10), ev(CONFIRMAR_ENTREGA, 13)), T0.plusDays(1));
        assertEquals(Duration.ofHours(11), e.manejo());
        assertEquals(Duration.ofHours(2), e.descanso());
        assertEquals(2, e.pausas());
        assertFalse(e.incumple());
    }

    @Test void pausaCortaTrasCompletarElLimiteEsDescansoInsuficiente() {
        var e = ReporteJornadasService.evaluar(viaje(5, 1, "Ana", "FINALIZADO",
                ev(INICIO_TRAYECTO, 0), ev(PAUSA_DESCANSO, 7.5), ev(REANUDAR, 8), ev(CONFIRMAR_ENTREGA, 12)), T0.plusDays(1));
        assertFalse(e.excesoManejo());
        assertTrue(e.descansoInsuficiente());
        assertEquals("Descanso insuficiente", e.resultado());
    }

    @Test void tramoAbiertoSeCuentaHastaAhora() {
        var e = ReporteJornadasService.evaluar(viaje(6, 1, "Ana", "EN_CURSO", ev(INICIO_TRAYECTO, 0)), T0.plusHours(3));
        assertTrue(e.abierto());
        assertEquals(Duration.ofHours(3), e.manejo());
        assertFalse(e.incumple());
    }

    @Test void contenidoAgrupaPorOperadorYGeneraPdf() throws Exception {
        var viajes = List.of(
                viaje(10, 2, "Luis", "FINALIZADO", ev(INICIO_TRAYECTO, 0), ev(CONFIRMAR_ENTREGA, 9)),
                viaje(11, 1, "Ana", "FINALIZADO", ev(INICIO_TRAYECTO, 0), ev(CONFIRMAR_ENTREGA, 4)),
                viaje(12, 1, "Ana", "PAUSADO", ev(INICIO_TRAYECTO, 0), ev(PAUSA_DESCANSO, 7.5)));
        var contenido = ReporteJornadasService.preparar(viajes, T0.plusHours(8));
        assertEquals(3, contenido.viajes());
        assertEquals(1, contenido.incumplimientos());
        assertEquals("#11", contenido.filas().get(0).get("viaje"), "Ana va antes que Luis");
        assertEquals("#10", contenido.filas().get(2).get("viaje"));
        assertTrue(contenido.metricas().contains("Con incumplimiento: 1"));

        var impresion = new ReporteJornadasService().imprimir(contenido, "Empresa de prueba",
                ZonedDateTime.of(T0.plusHours(8), ZoneId.of("America/Managua")), "Estado: Todos");
        assertFalse(impresion.getPages().isEmpty());
        byte[] pdf = JasperExportManager.exportReportToPdf(impresion);
        assertTrue(pdf.length > 1000);
        String destino = System.getProperty("fleetroute.jornadas.pdf");
        if (destino != null) java.nio.file.Files.write(java.nio.file.Path.of(destino), pdf);
    }

    @Test void reporteVacioTambienSeGenera() {
        var contenido = ReporteJornadasService.preparar(List.of(), T0);
        var impresion = new ReporteJornadasService().imprimir(contenido, "Empresa de prueba",
                ZonedDateTime.of(T0, ZoneId.of("America/Managua")), "Estado: Todos");
        assertEquals(1, impresion.getPages().size());
    }
}
