package com.fleetroute.api.service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Reglas de solo lectura de la jornada que ya usa la cabina JavaFX.
 * La bitácora es la fuente de verdad para calcular manejo y descanso.
 */
public final class JornadaCalculator {

    private static final BigDecimal UMBRAL_FATIGA = new BigDecimal("0.80");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    public record Datos(
            LocalDateTime fechaLimite,
            BigDecimal tiempoEstimado,
            BigDecimal horasMaxConduccion,
            BigDecimal horasMinDescanso) {
    }

    public record Evento(
            int numero,
            String tipo,
            LocalDateTime fechaHora,
            String observacion) {
    }

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

    public record Resultado(
            Duration manejo,
            Duration descanso,
            Duration descansoRestante,
            Duration descansoMinimo,
            int porcentajeManejo,
            Paso paso,
            Semaforo semaforo,
            String mensaje,
            boolean puedeReanudar,
            LocalDateTime llegadaProyectada) {

        public String manejoTexto() {
            return texto(manejo);
        }

        public String descansoTexto() {
            return texto(descanso);
        }

        public String descansoRestanteTexto() {
            return texto(descansoRestante);
        }

        public String descansoMinimoTexto() {
            return texto(descansoMinimo);
        }
    }

    private JornadaCalculator() {
    }

    public static Resultado calcular(Datos datos, List<Evento> eventos, LocalDateTime ahora) {
        Duration manejo = Duration.ZERO;
        Duration descanso = Duration.ZERO;
        Duration descansoEnPausa = Duration.ZERO;
        LocalDateTime inicioTramo = null;
        LocalDateTime inicioPausa = null;
        String ultimoTipo = null;

        for (Evento evento : eventos) {
            String tipo = evento.tipo();
            LocalDateTime marca = evento.fechaHora();
            if (marca == null) {
                continue;
            }
            ultimoTipo = tipo;
            switch (tipo) {
                case "INICIO_TRAYECTO" -> {
                    if (inicioTramo != null) {
                        manejo = manejo.plus(Duration.between(inicioTramo, marca));
                    }
                    inicioTramo = marca;
                    inicioPausa = null;
                    // Cada viaje empieza un ciclo nuevo de descanso.
                    descanso = Duration.ZERO;
                    descansoEnPausa = Duration.ZERO;
                }
                case "PAUSA_DESCANSO" -> {
                    if (inicioTramo != null) {
                        manejo = manejo.plus(Duration.between(inicioTramo, marca));
                        inicioTramo = null;
                    }
                    inicioPausa = marca;
                }
                case "REANUDAR" -> {
                    if (inicioPausa != null) {
                        descansoEnPausa = Duration.between(inicioPausa, marca);
                        descanso = descanso.plus(descansoEnPausa);
                        inicioPausa = null;
                    }
                    inicioTramo = marca;
                }
                case "CONFIRMAR_ENTREGA" -> {
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
                    // Un tipo desconocido no altera los acumulados.
                }
            }
        }

        if (inicioTramo != null) {
            manejo = manejo.plus(Duration.between(inicioTramo, ahora));
        }
        if (inicioPausa != null) {
            descansoEnPausa = Duration.between(inicioPausa, ahora);
            descanso = descanso.plus(descansoEnPausa);
        }

        Paso paso = switch (ultimoTipo == null ? "" : ultimoTipo) {
            case "INICIO_TRAYECTO", "REANUDAR" -> Paso.EN_MARCHA;
            case "PAUSA_DESCANSO" -> Paso.EN_PAUSA;
            case "CONFIRMAR_ENTREGA" -> Paso.FINALIZADO;
            default -> Paso.SIN_INICIAR;
        };

        Duration minimo = descansoMinimo(datos.horasMinDescanso());
        Duration restante = minimo.minus(descanso);
        if (restante.isNegative()) {
            restante = Duration.ZERO;
        }

        BigDecimal maximo = datos.horasMaxConduccion() == null
                ? BigDecimal.ZERO : datos.horasMaxConduccion();
        int porcentaje = maximo.signum() <= 0 ? 0
                : (int) Math.round(manejo.toMinutes() / 60.0 * 100.0 / maximo.doubleValue());

        double horasManejo = manejo.toMinutes() / 60.0;
        double horasRestantes = Math.max(0,
                (datos.tiempoEstimado() == null ? 0 : datos.tiempoEstimado().doubleValue()) - horasManejo);
        LocalDateTime llegada = ahora.plusSeconds(Math.round(horasRestantes * 3600));

        Semaforo semaforo;
        String mensaje;
        if (datos.fechaLimite() != null && llegada.isAfter(datos.fechaLimite())) {
            semaforo = Semaforo.RETRASO_SLA;
            mensaje = "Llegada proyectada " + HORA.format(llegada)
                    + ", después del límite del cliente (" + HORA.format(datos.fechaLimite())
                    + "). Avise al administrador.";
        } else if (maximo.signum() > 0
                && BigDecimal.valueOf(horasManejo).compareTo(maximo.multiply(UMBRAL_FATIGA)) >= 0) {
            semaforo = Semaforo.DESCANSO_PENDIENTE;
            mensaje = "Manejo acumulado " + texto(manejo) + " (" + porcentaje + " % de "
                    + maximo.stripTrailingZeros().toPlainString()
                    + " h). Programe una pausa de descanso.";
        } else {
            semaforo = Semaforo.A_TIEMPO;
            mensaje = datos.fechaLimite() == null
                    ? "Dentro del tiempo de manejo permitido."
                    : "Dentro del tiempo: llegada proyectada " + HORA.format(llegada)
                    + ", límite del cliente " + HORA.format(datos.fechaLimite()) + ".";
        }

        return new Resultado(manejo, descanso, restante, minimo, porcentaje, paso, semaforo, mensaje,
                paso == Paso.EN_PAUSA, llegada);
    }

    private static Duration descansoMinimo(BigDecimal horas) {
        if (horas == null || horas.signum() <= 0) {
            return Duration.ZERO;
        }
        return Duration.ofMinutes(Math.max(0, Math.round(horas.doubleValue() * 60)));
    }

    private static String texto(Duration duracion) {
        long minutos = Math.max(0, duracion.toMinutes());
        return (minutos / 60) + " h " + String.format(Locale.ROOT, "%02d", minutos % 60) + " min";
    }
}
