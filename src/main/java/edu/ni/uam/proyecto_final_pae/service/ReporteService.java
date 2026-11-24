package edu.ni.uam.proyecto_final_pae.service;

import edu.ni.uam.proyecto_final_pae.config.ZonaHoraria;
import edu.ni.uam.proyecto_final_pae.dao.EmpresaDAO;
import edu.ni.uam.proyecto_final_pae.dao.ReporteDAO;
import edu.ni.uam.proyecto_final_pae.model.RolUsuario;
import edu.ni.uam.proyecto_final_pae.model.Usuario;
import edu.ni.uam.proyecto_final_pae.reportes.*;
import edu.ni.uam.proyecto_final_pae.security.SesionActual;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.*;

public class ReporteService {
    private final ReporteDAO dao;

    public ReporteService() { this(new ReporteDAO()); }
    public ReporteService(ReporteDAO dao) { this.dao = Objects.requireNonNull(dao); }

    public static Usuario requerirPermiso() {
        Usuario usuario = SesionActual.requerir(RolUsuario.ADMIN);
        if (!Boolean.TRUE.equals(usuario.getEsPrincipal()) && !Boolean.TRUE.equals(usuario.getPuedeVerReportes())) {
            throw new SecurityException("No tiene permiso para consultar o exportar reportes financieros.");
        }
        return usuario;
    }

    public CatalogosReporte catalogos() {
        Usuario solicitante = requerirPermiso();
        CatalogosReporte opciones = dao.catalogos();
        if (requerirPermiso() != solicitante) throw new SecurityException("La sesión cambió al cargar los filtros.");
        return opciones;
    }

    public ResultadoReporte generar(FiltroReporte filtro) {
        Usuario solicitante = requerirPermiso();
        var empresa = new EmpresaDAO().obtener().orElseThrow(() ->
                new IllegalStateException("Configure los datos de la empresa antes de generar reportes."));
        List<ViajeReporte> viajes = dao.consultar(filtro);
        ResultadoReporte resultado = preparar(filtro, viajes, empresa.getNombre(), empresa.getMoneda(),
                ZonedDateTime.now(ZonaHoraria.actual()));
        if (requerirPermiso() != solicitante) throw new SecurityException("La sesión cambió durante la generación.");
        return resultado;
    }

    /** Cálculo puro sobre la misma proyección; reutilizable sin JavaFX ni Hibernate. */
    public static ResultadoReporte preparar(FiltroReporte filtro, List<ViajeReporte> viajes, String empresa,
                                            String moneda, ZonedDateTime generado) {
        if (viajes.size() > FiltroReporte.MAX_FILAS) throw new IllegalArgumentException("Se excedió el límite de viajes.");
        BigDecimal total = viajes.stream().map(ViajeReporte::importe).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal promedio = viajes.isEmpty() ? BigDecimal.ZERO.setScale(2)
                : total.divide(BigDecimal.valueOf(viajes.size()), 2, RoundingMode.HALF_UP);
        List<FilaReporte> filas = switch (filtro.tipo()) {
            case OPERADOR -> agrupar(viajes, true);
            case UNIDAD -> agrupar(viajes, false);
            case RESUMEN, EVOLUCION -> periodos(filtro, viajes);
            case DETALLE -> List.of();
        };
        List<FilaReporte> series = filas;
        if (!filtro.tipo().temporal() && filas.size() > 10) {
            series = new ArrayList<>(filas.subList(0, 10));
            var resto = filas.subList(10, filas.size());
            series.add(new FilaReporte("Otros (" + resto.size() + ")", resto.stream().mapToLong(FilaReporte::viajes).sum(),
                    resto.stream().map(FilaReporte::total).reduce(BigDecimal.ZERO, BigDecimal::add)));
        }
        return new ResultadoReporte(filtro, empresa, moneda, generado, viajes, filas, series, total, viajes.size(), promedio);
    }

    private static List<FilaReporte> agrupar(List<ViajeReporte> viajes, boolean operador) {
        Map<Integer, List<ViajeReporte>> grupos = new LinkedHashMap<>();
        for (ViajeReporte viaje : viajes) grupos.computeIfAbsent(operador ? viaje.operadorId() : viaje.unidadId(),
                id -> new ArrayList<>()).add(viaje);
        return grupos.entrySet().stream().map(entrada -> {
            ViajeReporte primero = entrada.getValue().get(0);
            return new FilaReporte((operador ? primero.operador() : primero.unidad()) + " (#" + entrada.getKey() + ")",
                    entrada.getValue().size(), entrada.getValue().stream().map(ViajeReporte::importe)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
        }).sorted(Comparator.comparing(FilaReporte::total).reversed().thenComparing(FilaReporte::categoria)).toList();
    }

    private static List<FilaReporte> periodos(FiltroReporte filtro, List<ViajeReporte> viajes) {
        Map<LocalDate, List<ViajeReporte>> grupos = new LinkedHashMap<>();
        for (LocalDate dia = filtro.agrupacion().inicio(filtro.desde()); !dia.isAfter(filtro.hasta());
             dia = filtro.agrupacion().siguiente(dia)) grupos.put(dia, new ArrayList<>());
        for (ViajeReporte viaje : viajes) grupos.get(filtro.agrupacion().inicio(viaje.fin().toLocalDate())).add(viaje);
        return grupos.entrySet().stream().map(entrada -> {
            LocalDate inicio = entrada.getKey().isBefore(filtro.desde()) ? filtro.desde() : entrada.getKey();
            LocalDate fin = filtro.agrupacion().siguiente(entrada.getKey()).minusDays(1);
            if (fin.isAfter(filtro.hasta())) fin = filtro.hasta();
            String etiqueta = inicio.equals(fin) ? inicio.toString() : inicio + " a " + fin;
            return new FilaReporte(etiqueta, entrada.getValue().size(), entrada.getValue().stream()
                    .map(ViajeReporte::importe).reduce(BigDecimal.ZERO, BigDecimal::add));
        }).toList();
    }
}
