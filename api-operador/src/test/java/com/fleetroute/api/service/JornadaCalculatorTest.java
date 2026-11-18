package com.fleetroute.api.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JornadaCalculatorTest {

    private static final LocalDateTime BASE = LocalDateTime.of(2026, 1, 15, 8, 0);
    private static final BigDecimal OCHO_HORAS = new BigDecimal("8");

    @Test
    void sinEventosNoAcumulaTiempo() {
        var resultado = JornadaCalculator.calcular(
                new JornadaCalculator.Datos(null, new BigDecimal("4"), OCHO_HORAS, OCHO_HORAS),
                List.of(), BASE);

        assertEquals(JornadaCalculator.Paso.SIN_INICIAR, resultado.paso());
        assertEquals(Duration.ZERO, resultado.manejo());
        assertEquals(JornadaCalculator.Semaforo.A_TIEMPO, resultado.semaforo());
        assertFalse(resultado.puedeReanudar());
    }

    @Test
    void sumaManejoYDescansoEntreEventos() {
        var eventos = List.of(
                new JornadaCalculator.Evento(1, "INICIO_TRAYECTO", BASE, null),
                new JornadaCalculator.Evento(2, "PAUSA_DESCANSO", BASE.plusHours(1), null),
                new JornadaCalculator.Evento(3, "REANUDAR", BASE.plusMinutes(90), null)
        );

        var resultado = JornadaCalculator.calcular(
                new JornadaCalculator.Datos(null, new BigDecimal("4"), OCHO_HORAS, OCHO_HORAS),
                eventos, BASE.plusMinutes(150));

        assertEquals(JornadaCalculator.Paso.EN_MARCHA, resultado.paso());
        assertEquals(Duration.ofHours(2), resultado.manejo());
        assertEquals(Duration.ofMinutes(30), resultado.descanso());
        assertEquals(Duration.ofMinutes(450), resultado.descansoRestante());
        assertEquals("2 h 00 min", resultado.manejoTexto());
        assertEquals("0 h 30 min", resultado.descansoTexto());
        assertFalse(resultado.puedeReanudar());
    }

    @Test
    void detectaRetrasoSobreElLimiteDelCliente() {
        var resultado = JornadaCalculator.calcular(
                new JornadaCalculator.Datos(BASE, new BigDecimal("4"), OCHO_HORAS, OCHO_HORAS),
                List.of(), BASE);

        assertEquals(JornadaCalculator.Semaforo.RETRASO_SLA, resultado.semaforo());
        assertTrue(resultado.mensaje().contains("después del límite"));
    }

    @Test
    void avisaCuandoElManejoLlegaAlOchentaPorCiento() {
        var eventos = List.of(new JornadaCalculator.Evento(1, "INICIO_TRAYECTO", BASE, null));
        var resultado = JornadaCalculator.calcular(
                new JornadaCalculator.Datos(null, new BigDecimal("10"), OCHO_HORAS, OCHO_HORAS),
                eventos, BASE.plusHours(7));

        assertEquals(88, resultado.porcentajeManejo());
        assertEquals(JornadaCalculator.Semaforo.DESCANSO_PENDIENTE, resultado.semaforo());
    }
}
