package edu.ni.uam.proyecto_final_pae.service;

import edu.ni.uam.proyecto_final_pae.config.PersistenciaException;
import edu.ni.uam.proyecto_final_pae.dao.FlotaDAO;
import edu.ni.uam.proyecto_final_pae.dao.RegistroJornadaDAO;
import edu.ni.uam.proyecto_final_pae.model.RegistroJornada;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Reglas de jornada del operador (RF-06, RF-07 y RF-08) y semaforo del viaje.
 *
 * <ul>
 *   <li>RF-06: la cabina solo permite iniciar un tramo si no hay otro abierto y confirmar la
 *       entrega si el viaje esta en curso.</li>
 *   <li>RF-07: no se puede REANUDAR hasta acumular el descanso minimo del operador.</li>
 *   <li>RF-08: se avisa cuando el manejo acumulado llega al 80 % del maximo permitido.</li>
 * </ul>
 *
 * <p>Las horas no se guardan en columnas: se calculan sumando los intervalos de la bitacora
 * (registro_jornada), que es la unica fuente de verdad.</p>
 */
public class JornadaService {

    public static final String INICIO_TRAYECTO = "INICIO_TRAYECTO";
    public static final String PAUSA_DESCANSO = "PAUSA_DESCANSO";
    public static final String REANUDAR = "REANUDAR";
    public static final String CONFIRMAR_ENTREGA = "CONFIRMAR_ENTREGA";

    /** Misma zona que hibernate.jdbc.time_zone. */
    public static final ZoneId ZONA = ZoneId.of("America/Managua");

    private static final BigDecimal UMBRAL_FATIGA = new BigDecimal("0.80");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    private final FlotaDAO flotaDAO = new FlotaDAO();
    private final RegistroJornadaDAO registroDAO = new RegistroJornadaDAO();

    /** Semaforo del viaje: los tres estados que pide el RF-08. */
    public enum Semaforo {
        A_TIEMPO("A tiempo", "#1b7f3b"),
        DESCANSO_PENDIENTE("Descanso pendiente", "#b8860b"),
        RETRASO_SLA("Retraso (SLA)", "#b22222");

        private final String etiqueta;
        private final String color;

        Semaforo(String etiqueta, String color) {
            this.etiqueta = etiqueta;
            this.color = color;
        }

        public String getEtiqueta() {
            return etiqueta;
        }

        public String getColor() {
            return color;
        }
    }

    /** Paso actual de la jornada, deducido del ultimo evento registrado. */
    public enum Paso {
        SIN_INICIAR("Sin iniciar"),
        EN_MARCHA("En marcha"),
        EN_PAUSA("En pausa"),
        FINALIZADO("Finalizado");

        private final String etiqueta;

        Paso(String etiqueta) {
            this.etiqueta = etiqueta;
        }

        public String getEtiqueta() {
            return etiqueta;
        }
    }

    /** Resultado del calculo: horas acumuladas, paso actual y semaforo. */
    public record Resumen(
            FlotaDAO.FilaViaje viaje,
            Duration manejo,
            Duration descanso,
            Duration descansoEnPausa,
            Paso paso,
            Semaforo semaforo,
            String mensaje,
            boolean puedeReanudar) {

        public String manejoTexto() {
            return texto(manejo);
        }

        public String descansoTexto() {
            return texto(descanso);
        }

        /** Porcentaje de manejo respecto al maximo permitido al operador. */
        public int porcentajeManejo() {
            if (viaje.horasMaxConduccion() == null || viaje.horasMaxConduccion().signum() <= 0) {
                return 0;
            }
            double porcentaje = manejo.toMinutes() / 60.0 * 100.0 / viaje.horasMaxConduccion().doubleValue();
            return (int) Math.round(porcentaje);
        }
    }

    // ------------------------------------------------------------------ calculo

    /** Calcula el resumen de un viaje a partir de su bitacora. */
    public Resumen resumen(int idViaje) {
        FlotaDAO.FilaViaje viaje = flotaDAO.datosViaje(idViaje)
                .orElseThrow(() -> new PersistenciaException("No existe el viaje " + idViaje + ".", null));
        return calcular(viaje, registroDAO.listarPorViaje(idViaje), LocalDateTime.now(ZONA));
    }

    /**
     * Suma los intervalos de la bitacora: de INICIO_TRAYECTO/REANUDAR a PAUSA_DESCANSO/
     * CONFIRMAR_ENTREGA cuenta como manejo; de PAUSA_DESCANSO a REANUDAR cuenta como descanso.
     */
    public static Resumen calcular(FlotaDAO.FilaViaje viaje, List<RegistroJornada> eventos, LocalDateTime ahora) {
        Duration manejo = Duration.ZERO;
        Duration descanso = Duration.ZERO;
        Duration descansoEnPausa = Duration.ZERO;
        LocalDateTime inicioTramo = null;
        LocalDateTime inicioPausa = null;
        String ultimoTipo = null;

        for (RegistroJornada evento : eventos) {
            String tipo = evento.getTipoEvento().getNombre();
            LocalDateTime marca = evento.getFechaHora();
            ultimoTipo = tipo;
            switch (tipo) {
                case INICIO_TRAYECTO -> {
                    if (inicioTramo != null) {
                        manejo = manejo.plus(Duration.between(inicioTramo, marca));
                    }
                    inicioTramo = marca;
                    inicioPausa = null;
                    // Cada viaje empieza un ciclo nuevo de descanso.
                    descanso = Duration.ZERO;
                    descansoEnPausa = Duration.ZERO;
                }
                case PAUSA_DESCANSO -> {
                    if (inicioTramo != null) {
                        manejo = manejo.plus(Duration.between(inicioTramo, marca));
                        inicioTramo = null;
                    }
                    inicioPausa = marca;
                }
                case REANUDAR -> {
                    if (inicioPausa != null) {
                        descansoEnPausa = Duration.between(inicioPausa, marca);
                        descanso = descanso.plus(descansoEnPausa);
                        inicioPausa = null;
                    }
                    inicioTramo = marca;
                }
                case CONFIRMAR_ENTREGA -> {
                    if (inicioTramo != null) {
                        manejo = manejo.plus(Duration.between(inicioTramo, marca));
                        inicioTramo = null;
                    }
                    if (inicioPausa != null) {
                        descansoEnPausa = Duration.between(inicioPausa, marca);
                        descanso = descanso.plus(descansoEnPausa);
                        inicioPausa = null;
                    }
                }
                default -> {
                    // Tipo de evento no contemplado: no altera los acumulados.
                }
            }
        }

        // Tramo o pausa que siguen abiertos: se cuentan hasta "ahora".
        if (inicioTramo != null) {
            manejo = manejo.plus(Duration.between(inicioTramo, ahora));
        }
        if (inicioPausa != null) {
            descansoEnPausa = Duration.between(inicioPausa, ahora);
            descanso = descanso.plus(descansoEnPausa);
        }

        Paso paso = switch (ultimoTipo == null ? "" : ultimoTipo) {
            case INICIO_TRAYECTO, REANUDAR -> Paso.EN_MARCHA;
            case PAUSA_DESCANSO -> Paso.EN_PAUSA;
            case CONFIRMAR_ENTREGA -> Paso.FINALIZADO;
            default -> Paso.SIN_INICIAR;
        };

        BigDecimal maximo = viaje.horasMaxConduccion() == null ? BigDecimal.ZERO : viaje.horasMaxConduccion();
        BigDecimal minimoDescanso = viaje.horasMinDescanso() == null ? BigDecimal.ZERO : viaje.horasMinDescanso();
        Duration minimo = Duration.ofMinutes(Math.max(0, Math.round(minimoDescanso.doubleValue() * 60)));

        // Proyeccion de llegada: las horas de manejo que faltan se suman al momento actual.
        double horasManejo = manejo.toMinutes() / 60.0;
        double horasRestantes = Math.max(0,
                (viaje.tiempoEstimado() == null ? 0 : viaje.tiempoEstimado().doubleValue()) - horasManejo);
        LocalDateTime llegada = ahora.plusSeconds(Math.round(horasRestantes * 3600));

        Semaforo semaforo;
        String mensaje;
        if (viaje.fechaLimite() != null && llegada.isAfter(viaje.fechaLimite())) {
            semaforo = Semaforo.RETRASO_SLA;
            mensaje = "Llegada proyectada " + HORA.format(llegada) + ", despues del limite del cliente ("
                    + HORA.format(viaje.fechaLimite()) + "). Avise al administrador.";
        } else if (maximo.signum() > 0
                && BigDecimal.valueOf(horasManejo).compareTo(maximo.multiply(UMBRAL_FATIGA)) >= 0) {
            semaforo = Semaforo.DESCANSO_PENDIENTE;
            mensaje = "Manejo acumulado " + texto(manejo) + " (" + Math.round(horasManejo * 100.0
                    / maximo.doubleValue()) + " % de " + maximo.stripTrailingZeros().toPlainString()
                    + " h). Programe una pausa de descanso.";
        } else {
            semaforo = Semaforo.A_TIEMPO;
            mensaje = viaje.fechaLimite() == null
                    ? "Dentro del tiempo de manejo permitido."
                    : "Dentro del tiempo: llegada proyectada " + HORA.format(llegada)
                            + ", limite del cliente " + HORA.format(viaje.fechaLimite()) + ".";
        }

        boolean puedeReanudar = paso == Paso.EN_PAUSA && descanso.compareTo(minimo) >= 0;
        return new Resumen(viaje, manejo, descanso, descansoEnPausa, paso, semaforo, mensaje, puedeReanudar);
    }

    /** Bitacora del viaje (para mostrarla en la cabina). */
    public List<RegistroJornada> eventos(int idViaje) {
        return registroDAO.listarPorViaje(idViaje);
    }

    /** Paso actual sin recalcular horas (consulta el ultimo evento). */
    public Paso paso(int idViaje) {
        RegistroJornada ultimo = registroDAO.ultimo(idViaje);
        if (ultimo == null) {
            return Paso.SIN_INICIAR;
        }
        return switch (ultimo.getTipoEvento().getNombre()) {
            case INICIO_TRAYECTO, REANUDAR -> Paso.EN_MARCHA;
            case PAUSA_DESCANSO -> Paso.EN_PAUSA;
            case CONFIRMAR_ENTREGA -> Paso.FINALIZADO;
            default -> Paso.SIN_INICIAR;
        };
    }

    // ------------------------------------------------------------------ acciones de la cabina

    /** RF-06: inicia el viaje registrando INICIO_TRAYECTO. */
    public Resumen iniciar(int idViaje) {
        Paso paso = paso(idViaje);
        if (paso == Paso.EN_MARCHA) {
            throw new PersistenciaException(
                    "Ya hay un tramo abierto: registre PAUSA o CONFIRMAR ENTREGA antes de iniciar otro.", null);
        }
        if (paso == Paso.FINALIZADO) {
            throw new PersistenciaException("El viaje ya fue entregado; no se puede iniciar otro tramo.", null);
        }
        registroDAO.registrar(idViaje, INICIO_TRAYECTO, LocalDateTime.now(ZONA),
                "Inicio de trayecto desde la cabina");
        flotaDAO.cambiarEstadoViaje(idViaje, "EN_CURSO");
        return resumen(idViaje);
    }

    /** RF-06: registra PAUSA_DESCANSO y deja el viaje en PAUSADO. */
    public Resumen pausar(int idViaje) {
        if (paso(idViaje) != Paso.EN_MARCHA) {
            throw new PersistenciaException("Solo se puede pausar mientras el tramo esta en marcha.", null);
        }
        registroDAO.registrar(idViaje, PAUSA_DESCANSO, LocalDateTime.now(ZONA), "Pausa de descanso");
        flotaDAO.cambiarEstadoViaje(idViaje, "PAUSADO");
        return resumen(idViaje);
    }

    /** RF-07: solo reanuda si el descanso acumulado alcanza el minimo del operador. */
    public Resumen reanudar(int idViaje) {
        if (paso(idViaje) != Paso.EN_PAUSA) {
            throw new PersistenciaException("Debe registrar una pausa (PAUSA_DESCANSO) antes de reanudar.", null);
        }
        Resumen actual = resumen(idViaje);
        if (!actual.puedeReanudar()) {
            BigDecimal minimo = actual.viaje().horasMinDescanso();
            throw new PersistenciaException("Descanso insuficiente (RF-07): lleva " + actual.descansoTexto()
                    + " acumuladas y el minimo del operador es "
                    + (minimo == null ? "0" : minimo.stripTrailingZeros().toPlainString()) + " h.", null);
        }
        registroDAO.registrar(idViaje, REANUDAR, LocalDateTime.now(ZONA), "Reanudar el manejo");
        flotaDAO.cambiarEstadoViaje(idViaje, "EN_CURSO");
        return resumen(idViaje);
    }

    /** RF-06 y RF-09: cierra el viaje y activa el viaje encadenado si existe. */
    public Resumen confirmarEntrega(int idViaje) {
        Paso paso = paso(idViaje);
        if (paso == Paso.FINALIZADO) {
            throw new PersistenciaException("La entrega de este viaje ya fue confirmada.", null);
        }
        if (paso == Paso.SIN_INICIAR) {
            throw new PersistenciaException("El viaje aun no tiene eventos de jornada.", null);
        }
        FlotaDAO.FilaViaje viaje = flotaDAO.datosViaje(idViaje)
                .orElseThrow(() -> new PersistenciaException("No existe el viaje " + idViaje + ".", null));

        registroDAO.registrar(idViaje, CONFIRMAR_ENTREGA, LocalDateTime.now(ZONA),
                "Entrega confirmada desde la cabina");
        flotaDAO.cambiarEstadoViaje(idViaje, "FINALIZADO");

        Optional<FlotaDAO.FilaViaje> encadenado = flotaDAO.viajeEncadenado(idViaje);
        if (encadenado.isPresent()) {
            flotaDAO.cambiarEstadoViaje(encadenado.get().idViaje(), "EN_CURSO");
        } else {
            flotaDAO.liberarRecursos(viaje.idUnidad(), viaje.idOperador());
        }
        return resumen(idViaje);
    }

    // ------------------------------------------------------------------ apoyo

    private static String texto(Duration duracion) {
        long minutos = Math.max(0, duracion.toMinutes());
        return minutos / 60 + " h " + String.format("%02d", minutos % 60) + " min";
    }
}
