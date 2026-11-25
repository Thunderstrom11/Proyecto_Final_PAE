package edu.ni.uam.proyecto_final_pae.service;

import edu.ni.uam.proyecto_final_pae.model.RolUsuario;
import edu.ni.uam.proyecto_final_pae.model.Usuario;
import edu.ni.uam.proyecto_final_pae.reportes.*;
import edu.ni.uam.proyecto_final_pae.security.SesionActual;
import net.sf.jasperreports.engine.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import static org.junit.jupiter.api.Assertions.*;

class ReporteEntregasServiceTest {
    private final LocalDate dia = LocalDate.of(2026, 6, 12);
    private final LocalDateTime limite = dia.atTime(12, 0);
    private final ZonedDateTime generado = limite.atZone(ZoneId.of("America/Managua"));
    private final ReporteEntregasService servicio = new ReporteEntregasService();

    @BeforeEach void iniciar() {
        Usuario usuario = new Usuario();
        usuario.setRol(RolUsuario.ADMIN);
        usuario.setPuedeVerReportes(false);
        SesionActual.iniciar(usuario);
    }

    @AfterEach void cerrar() { SesionActual.cerrar(); }

    private FiltroReporte filtro() {
        return new FiltroReporte(TipoReporte.CUMPLIMIENTO_ENTREGAS, dia, dia, null, null, null,
                "", null, TipoGrafico.NINGUNO);
    }

    private EntregaReporte entrega(int id, LocalDateTime plazo, LocalDateTime fin) {
        return new EntregaReporte(id, "CARGA-" + id, "Distribuidora del Norte", "María López", "U-001", plazo, fin);
    }

    @Test void incluyeLaHoraExactaDelPlazoYExcluyeFechasIncompletasDelPorcentaje() {
        var anticipada = entrega(1, limite, limite.minusHours(1));
        var exacta = entrega(2, limite, limite);
        var tardia = entrega(3, limite, limite.plusSeconds(1));
        var resumen = ResumenEntregas.de(List.of(anticipada, exacta, tardia, entrega(4, null, limite)));
        assertTrue(anticipada.aTiempo());
        assertTrue(exacta.aTiempo());
        assertEquals("Tardía", tardia.resultado());
        assertEquals("< 1 min", tardia.atraso());
        assertEquals(new ResumenEntregas(4, 2, 1, 1), resumen);
        assertEquals("66,7 %", resumen.porcentaje());
        assertEquals("25 h 15 min", entrega(5, limite, limite.plusDays(1).plusMinutes(75)).atraso());
        assertEquals("Sin datos", entrega(6, limite, null).resultado());
    }

    @Test void exigeFechasDeFinalizacionYSoloViajesFinalizados() {
        assertFalse(TipoReporte.CUMPLIMIENTO_ENTREGAS.financiero());
        assertTrue(TipoReporte.CUMPLIMIENTO_ENTREGAS.detallado());
        assertEquals(EstadoReporte.FINALIZADO, filtro().estado());
        assertEquals(dia.plusDays(1).atStartOfDay(), filtro().hastaExclusivo());
        assertTrue(filtro().descripcion().startsWith("Finalización: "));
        assertThrows(IllegalArgumentException.class, () -> new FiltroReporte(TipoReporte.CUMPLIMIENTO_ENTREGAS,
                null, null, null, null, null, "", null, TipoGrafico.NINGUNO));
        assertThrows(IllegalArgumentException.class, () -> new FiltroReporte(TipoReporte.CUMPLIMIENTO_ENTREGAS,
                dia, dia, null, null, null, "", null, TipoGrafico.NINGUNO, EstadoReporte.TODOS));
        var gestion = new FiltroReporte(TipoReporte.GESTION_VIAJES, null, null, null, null, null,
                "", null, TipoGrafico.NINGUNO);
        assertEquals(EstadoReporte.TODOS, gestion.estado());
    }

    @Test void generaJasperYPdfConElResumenYLasFechasRegistradas() throws Exception {
        var documento = servicio.generar(List.of(entrega(1, limite, limite), entrega(2, limite, limite.plusMinutes(90))),
                filtro(), "Transportes del Pacífico", generado);
        String texto = texto(documento.impresion());
        assertTrue(texto.contains("Cumplimiento de entregas"));
        assertTrue(texto.contains("50,0 %"));
        assertTrue(texto.contains("A tiempo"));
        assertTrue(texto.contains("Tardía"));
        assertTrue(texto.contains("1 h 30 min"));
        assertTrue(texto.contains("12/06/2026"));
        assertTrue(JasperExportManager.exportReportToPdf(documento.impresion()).length > 1000);
        assertTrue(documento.nombreArchivo().startsWith("FleetRoute_entregas_"));
        assertDoesNotThrow(() -> ReporteService.validarDocumento(documento));
    }

    @Test void elReporteVacioConservaEncabezadoYNoInventaUnPorcentaje() {
        var documento = servicio.generar(List.of(), filtro(), "Transportes del Pacífico", generado);
        assertEquals(1, documento.impresion().getPages().size());
        assertEquals("No aplica", documento.resumen().porcentaje());
        assertTrue(texto(documento.impresion()).contains("No hay entregas que coincidan con los filtros."));
    }

    @Test void conservaTodasLasFilasEnVariasPaginasConNombresLargos() {
        String nombre = "Centro de distribución y transporte de productos de consumo para la región del Pacífico y Norte de Nicaragua";
        var filas = IntStream.rangeClosed(1, 75).mapToObj(id -> new EntregaReporte(id,
                "CARGA-IDENTIFICADOR-LARGO-" + id, nombre, "María de los Ángeles López Rodríguez del Norte",
                "UNIDAD-001-REGIONAL", limite, limite.plusMinutes(id))).toList();
        var documento = servicio.generar(filas, filtro(), nombre, generado);
        assertTrue(documento.impresion().getPages().size() > 1);
        String texto = texto(documento.impresion());
        for (int id = 1; id <= 75; id++) assertTrue(texto.contains("#" + id), "Falta el viaje " + id);
        assertTrue(texto.contains(nombre));
        assertEquals(75, documento.resumen().viajes());
    }

    @Test void protegeLaGeneracionYElDocumentoAlCambiarDeSesion() {
        var documento = servicio.generar(List.of(entrega(1, limite, limite)), filtro(), "Transportes del Pacífico", generado);
        Usuario otro = new Usuario();
        otro.setRol(RolUsuario.ADMIN);
        SesionActual.iniciar(otro);
        assertThrows(SecurityException.class, () -> ReporteService.validarDocumento(documento));
        otro.setRol(RolUsuario.OPERADOR);
        assertThrows(SecurityException.class, () -> servicio.generar(filtro()));
        SesionActual.cerrar();
        assertThrows(SecurityException.class, () -> servicio.generar(filtro()));
    }

    private String texto(JasperPrint impresion) {
        List<String> textos = new ArrayList<>();
        for (JRPrintPage pagina : impresion.getPages()) extraer(pagina.getElements(), textos);
        return String.join("\n", textos);
    }

    private void extraer(List<JRPrintElement> elementos, List<String> textos) {
        for (JRPrintElement elemento : elementos) {
            if (elemento instanceof JRPrintText texto) textos.add(texto.getFullText());
            if (elemento instanceof JRPrintFrame marco) extraer(marco.getElements(), textos);
        }
    }
}
