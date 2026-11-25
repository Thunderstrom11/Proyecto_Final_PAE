package edu.ni.uam.proyecto_final_pae.service;

import edu.ni.uam.proyecto_final_pae.config.ZonaHoraria;
import edu.ni.uam.proyecto_final_pae.dao.EmpresaDAO;
import edu.ni.uam.proyecto_final_pae.dao.ReporteDAO;
import edu.ni.uam.proyecto_final_pae.model.RolUsuario;
import edu.ni.uam.proyecto_final_pae.model.Usuario;
import edu.ni.uam.proyecto_final_pae.reportes.DocumentoJornadas;
import edu.ni.uam.proyecto_final_pae.reportes.FiltroReporte;
import edu.ni.uam.proyecto_final_pae.reportes.ViajeJornada;
import edu.ni.uam.proyecto_final_pae.security.SesionActual;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Reporte de cumplimiento de jornada (RF-06 a RF-08): por cada viaje con bitácora compara el
 * tramo continuo de manejo más largo con las horas máximas del operador y el descanso acumulado
 * con el descanso que exigían los ciclos de manejo completados.
 */
public class ReporteJornadasService {
    /** Margen para la pausa automática, que se registra unos segundos después de llegar al límite. */
    public static final Duration TOLERANCIA = Duration.ofMinutes(5);
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private JasperReport plantilla;

    /** Resultado del análisis de la bitácora de un viaje. */
    public record Evaluacion(Duration manejo, Duration tramoMaximo, Duration descanso, Duration descansoRequerido,
                             int pausas, boolean excesoManejo, boolean descansoInsuficiente, boolean abierto) {
        public boolean incumple() { return excesoManejo || descansoInsuficiente; }

        public String resultado() {
            if (excesoManejo && descansoInsuficiente) return "Exceso de manejo y descanso insuficiente";
            if (excesoManejo) return "Exceso de manejo continuo";
            if (descansoInsuficiente) return "Descanso insuficiente";
            return "Conforme";
        }
    }

    /** Métricas del documento y filas listas para la plantilla. */
    public record Contenido(List<Map<String, ?>> filas, int viajes, int incumplimientos, int excesos,
                            int descansosInsuficientes, Duration manejo, Duration descanso) {
        public String metricas() {
            long conformes = viajes - incumplimientos;
            return "Viajes evaluados: " + viajes
                    + " | Conformes: " + conformes + (viajes == 0 ? "" : " (" + Math.round(conformes * 100.0 / viajes) + " %)")
                    + " | Con incumplimiento: " + incumplimientos
                    + " (exceso de manejo: " + excesos + ", descanso insuficiente: " + descansosInsuficientes + ")"
                    + "\nManejo total: " + texto(manejo) + " | Descanso total: " + texto(descanso);
        }
    }

    public DocumentoJornadas generar(FiltroReporte filtro) {
        Usuario solicitante = SesionActual.requerir(RolUsuario.ADMIN);
        List<ViajeJornada> viajes = new ReporteDAO().consultarJornadas(filtro);
        if (SesionActual.requerir(RolUsuario.ADMIN) != solicitante) throw new SecurityException("La sesión cambió durante la consulta.");
        var empresa = new EmpresaDAO().obtener().orElseThrow(() ->
                new IllegalStateException("Configure los datos de la empresa antes de generar reportes."));
        ZonedDateTime generado = ZonedDateTime.now(ZonaHoraria.actual());
        Contenido contenido = preparar(viajes, generado.toLocalDateTime());
        JasperPrint impresion = imprimir(contenido, empresa.getNombre(), generado, filtro.descripcion());
        if (SesionActual.requerir(RolUsuario.ADMIN) != solicitante) throw new SecurityException("La sesión cambió durante la generación.");
        return new DocumentoJornadas(impresion, generado, contenido.viajes(), contenido.incumplimientos(), solicitante);
    }

    /** Llena la plantilla sin consultar la base; reutilizable en pruebas. */
    public JasperPrint imprimir(Contenido contenido, String empresa, ZonedDateTime generado, String filtros) {
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("EMPRESA", empresa);
        parametros.put("GENERADO", "Generado: " + generado.format(FECHA) + " | " + generado.getZone());
        parametros.put("FILTROS", filtros);
        parametros.put("METRICAS", contenido.metricas());
        parametros.put("VACIO", contenido.filas().isEmpty());
        try {
            return JasperFillManager.fillReport(plantilla(), parametros, new JRMapCollectionDataSource(contenido.filas()));
        } catch (JRException | IOException e) {
            throw new IllegalStateException("No se pudo generar el reporte de jornadas.", e);
        }
    }

    /** Cálculo puro: evalúa cada viaje y arma las filas agrupadas por operador. */
    public static Contenido preparar(List<ViajeJornada> viajes, LocalDateTime ahora) {
        if (viajes.size() > FiltroReporte.MAX_FILAS) throw new IllegalArgumentException("Se excedió el límite de viajes.");
        List<ViajeJornada> ordenados = viajes.stream().sorted(Comparator
                .comparing((ViajeJornada v) -> v.operador() == null ? "" : v.operador(), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(ViajeJornada::operadorId, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(v -> inicio(v), Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(ViajeJornada::id)).toList();

        Map<ViajeJornada, Evaluacion> evaluaciones = new LinkedHashMap<>();
        for (ViajeJornada viaje : ordenados) evaluaciones.put(viaje, evaluar(viaje, ahora));

        // Subtotales por operador: se repiten en cada fila para que el pie de grupo los imprima.
        Map<Integer, String> subtotales = new HashMap<>();
        Map<Integer, List<Evaluacion>> porOperador = new LinkedHashMap<>();
        evaluaciones.forEach((v, e) -> porOperador.computeIfAbsent(v.operadorId(), id -> new ArrayList<>()).add(e));
        porOperador.forEach((id, lista) -> subtotales.put(id, "Subtotal del operador: " + lista.size() + " viaje(s) | Manejo "
                + texto(suma(lista, Evaluacion::manejo)) + " | Descanso " + texto(suma(lista, Evaluacion::descanso))
                + " | Con incumplimiento: " + lista.stream().filter(Evaluacion::incumple).count()));

        List<Map<String, ?>> filas = new ArrayList<>();
        int incumplimientos = 0, excesos = 0, descansos = 0;
        Duration manejo = Duration.ZERO, descanso = Duration.ZERO;
        for (var entrada : evaluaciones.entrySet()) {
            ViajeJornada v = entrada.getKey();
            Evaluacion e = entrada.getValue();
            if (e.incumple()) incumplimientos++;
            if (e.excesoManejo()) excesos++;
            if (e.descansoInsuficiente()) descansos++;
            manejo = manejo.plus(e.manejo());
            descanso = descanso.plus(e.descanso());
            Map<String, Object> fila = new HashMap<>();
            fila.put("operador", (v.operador() == null ? "Sin registro" : v.operador()) + " (#" + v.operadorId() + ")");
            fila.put("limites", "Límite: " + horas(v.horasMaxConduccion()) + " de manejo continuo | "
                    + horas(v.horasMinDescanso()) + " de descanso por cada ciclo de manejo completado");
            fila.put("subtotal", subtotales.get(v.operadorId()));
            fila.put("viaje", "#" + v.id());
            fila.put("carga", texto(v.carga()));
            fila.put("unidad", texto(v.unidad()));
            LocalDateTime inicio = inicio(v);
            fila.put("inicio", inicio == null ? "Sin registro" : inicio.format(FECHA));
            fila.put("estado", texto(v.estado()));
            fila.put("manejo", texto(e.manejo()) + (e.abierto() ? " *" : ""));
            fila.put("tramo", texto(e.tramoMaximo()) + porcentaje(e.tramoMaximo(), v.horasMaxConduccion()));
            fila.put("descanso", texto(e.descanso()) + " / " + texto(e.descansoRequerido()));
            fila.put("pausas", String.valueOf(e.pausas()));
            fila.put("resultado", e.resultado());
            fila.put("incumple", e.incumple());
            filas.add(fila);
        }
        return new Contenido(filas, filas.size(), incumplimientos, excesos, descansos, manejo, descanso);
    }

    /**
     * Recorre la bitácora con las mismas reglas que {@link JornadaService#calcular}: de INICIO_TRAYECTO o
     * REANUDAR a PAUSA_DESCANSO o CONFIRMAR_ENTREGA es manejo; de PAUSA_DESCANSO a REANUDAR es descanso.
     * Los tramos que siguen abiertos se cuentan hasta {@code ahora}.
     */
    public static Evaluacion evaluar(ViajeJornada viaje, LocalDateTime ahora) {
        Duration manejo = Duration.ZERO, tramoMaximo = Duration.ZERO, descanso = Duration.ZERO;
        LocalDateTime inicioTramo = null, inicioPausa = null;
        int pausas = 0;
        for (ViajeJornada.Evento evento : viaje.eventos()) {
            LocalDateTime marca = evento.fechaHora();
            switch (evento.tipo() == null ? "" : evento.tipo()) {
                case JornadaService.INICIO_TRAYECTO, JornadaService.REANUDAR -> {
                    if (inicioTramo != null) {
                        Duration tramo = Duration.between(inicioTramo, marca);
                        manejo = manejo.plus(tramo);
                        tramoMaximo = max(tramoMaximo, tramo);
                    }
                    if (inicioPausa != null) {
                        descanso = descanso.plus(Duration.between(inicioPausa, marca));
                        inicioPausa = null;
                    }
                    inicioTramo = marca;
                }
                case JornadaService.PAUSA_DESCANSO -> {
                    if (inicioTramo != null) {
                        Duration tramo = Duration.between(inicioTramo, marca);
                        manejo = manejo.plus(tramo);
                        tramoMaximo = max(tramoMaximo, tramo);
                        inicioTramo = null;
                    }
                    if (inicioPausa == null) {
                        inicioPausa = marca;
                        pausas++;
                    }
                }
                case JornadaService.CONFIRMAR_ENTREGA -> {
                    if (inicioTramo != null) {
                        Duration tramo = Duration.between(inicioTramo, marca);
                        manejo = manejo.plus(tramo);
                        tramoMaximo = max(tramoMaximo, tramo);
                        inicioTramo = null;
                    }
                    if (inicioPausa != null) {
                        descanso = descanso.plus(Duration.between(inicioPausa, marca));
                        inicioPausa = null;
                    }
                }
                default -> { }
            }
        }
        boolean abierto = inicioTramo != null || inicioPausa != null;
        if (inicioTramo != null && ahora.isAfter(inicioTramo)) {
            Duration tramo = Duration.between(inicioTramo, ahora);
            manejo = manejo.plus(tramo);
            tramoMaximo = max(tramoMaximo, tramo);
        }
        if (inicioPausa != null && ahora.isAfter(inicioPausa)) descanso = descanso.plus(Duration.between(inicioPausa, ahora));

        Duration limite = horasADuracion(viaje.horasMaxConduccion());
        Duration minimo = horasADuracion(viaje.horasMinDescanso());
        boolean exceso = limite.isPositive() && tramoMaximo.compareTo(limite.plus(TOLERANCIA)) > 0;
        // Cada vez que el manejo acumulado sobrepasa un múltiplo del límite se exige un descanso mínimo.
        long ciclos = 0;
        if (limite.isPositive()) {
            long efectivo = Math.max(0, manejo.minus(TOLERANCIA).toSeconds());
            ciclos = Math.max(0, (efectivo + limite.toSeconds() - 1) / limite.toSeconds() - 1);
        }
        Duration requerido = minimo.multipliedBy(ciclos);
        boolean insuficiente = ciclos > 0 && descanso.plus(TOLERANCIA).compareTo(requerido) < 0;
        return new Evaluacion(manejo, tramoMaximo, descanso, requerido, pausas, exceso, insuficiente, abierto);
    }

    private static LocalDateTime inicio(ViajeJornada viaje) {
        if (viaje.fechaInicio() != null) return viaje.fechaInicio();
        return viaje.eventos().isEmpty() ? null : viaje.eventos().get(0).fechaHora();
    }

    private static Duration suma(List<Evaluacion> lista, java.util.function.Function<Evaluacion, Duration> campo) {
        return lista.stream().map(campo).reduce(Duration.ZERO, Duration::plus);
    }

    private static Duration max(Duration a, Duration b) { return a.compareTo(b) >= 0 ? a : b; }

    private static Duration horasADuracion(BigDecimal horas) {
        if (horas == null || horas.signum() <= 0) return Duration.ZERO;
        return Duration.ofSeconds(horas.multiply(BigDecimal.valueOf(3600)).longValue());
    }

    private static String porcentaje(Duration tramo, BigDecimal horasMax) {
        Duration limite = horasADuracion(horasMax);
        if (!limite.isPositive()) return "";
        return " (" + Math.round(tramo.toSeconds() * 100.0 / limite.toSeconds()) + " %)";
    }

    private static String horas(BigDecimal horas) {
        return horas == null ? "Sin definir" : horas.stripTrailingZeros().toPlainString() + " h";
    }

    private static String texto(String valor) { return valor == null || valor.isBlank() ? "Sin registro" : valor; }

    static String texto(Duration duracion) {
        long minutos = Math.max(0, duracion.toMinutes());
        long h = minutos / 60, m = minutos % 60;
        return h == 0 ? m + " min" : h + " h " + String.format("%02d", m) + " min";
    }

    private synchronized JasperReport plantilla() throws JRException, IOException {
        if (plantilla == null) {
            try (var entrada = getClass().getResourceAsStream("/edu/ni/uam/proyecto_final_pae/reportes/jornadas.jrxml")) {
                if (entrada == null) throw new IOException("No se encontró la plantilla del reporte de jornadas.");
                plantilla = JasperCompileManager.compileReport(entrada);
            }
        }
        return plantilla;
    }
}
