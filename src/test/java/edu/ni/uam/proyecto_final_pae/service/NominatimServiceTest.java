package edu.ni.uam.proyecto_final_pae.service;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NominatimServiceTest {
    private HttpServer servidor;
    private NominatimService servicio;
    private volatile int estado = 200;
    private volatile String contenido;
    private final List<String> consultas = new CopyOnWriteArrayList<>();
    private final List<Long> tiempos = new CopyOnWriteArrayList<>();

    @BeforeEach void iniciar() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/", intercambio -> {
            consultas.add(intercambio.getRequestURI().toString());
            tiempos.add(System.nanoTime());
            String cuerpo = contenido != null ? contenido : intercambio.getRequestURI().getPath().equals("/reverse")
                    ? "{\"display_name\":\"José Pérez, Managua\",\"lat\":\"12.1\",\"lon\":\"-86.2\"}" : "[]";
            byte[] bytes = cuerpo.getBytes(StandardCharsets.UTF_8);
            intercambio.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
            intercambio.sendResponseHeaders(estado, bytes.length);
            try (var salida = intercambio.getResponseBody()) { salida.write(bytes); }
        });
        servidor.start();
        servicio = cliente(Duration.ZERO);
    }

    private NominatimService cliente(Duration intervalo) {
        return new NominatimService(URI.create("http://127.0.0.1:" + servidor.getAddress().getPort()), intervalo);
    }

    @AfterEach void detener() { servicio.close(); servidor.stop(0); }

    @Test void convierteReferenciaYConservaAcentos() throws Exception {
        var resultado = servicio.referencia(12.1, -86.2).get(3, TimeUnit.SECONDS);
        assertEquals("José Pérez, Managua", resultado.path("display_name").asText());
        assertEquals("12.1", resultado.path("lat").asText());
        assertEquals("-86.2", resultado.path("lon").asText());
        assertTrue(consultas.get(0).contains("lat=12.1&lon=-86.2"));
    }

    @Test void busquedaVaciaEsUnResultadoValidoYCodificaFiltros() throws Exception {
        assertTrue(servicio.buscar("José & hijos", "Nicaragua", new double[]{10, -88, 15, -82})
                .get(3, TimeUnit.SECONDS).isEmpty());
        String consulta = URLDecoder.decode(consultas.get(0), StandardCharsets.UTF_8);
        assertTrue(consulta.contains("q=José & hijos, Nicaragua"));
        assertTrue(consulta.contains("viewbox=-88.0,15.0,-82.0,10.0&bounded=1"));
    }

    @Test void errorHttpTerminaLaConsultaEnLugarDeDejarlaPendiente() {
        estado = 429;
        var error = assertThrows(ExecutionException.class,
                () -> servicio.referencia(12, -86).get(3, TimeUnit.SECONDS));
        assertInstanceOf(IOException.class, error.getCause());
        assertTrue(error.getCause().getMessage().contains("HTTP 429"));
    }

    @Test void rechazaJsonInvalido() {
        contenido = "esto no es JSON";
        var error = assertThrows(ExecutionException.class,
                () -> servicio.buscar("Managua", "Nicaragua", null).get(3, TimeUnit.SECONDS));
        assertInstanceOf(IOException.class, error.getCause());
    }

    @Test void rechazaErroresYFormasIncorrectasConEstado200() {
        contenido = "{\"error\":\"No reference found\"}";
        assertThrows(ExecutionException.class, () -> servicio.referencia(12, -86).get(3, TimeUnit.SECONDS));
        contenido = "{}";
        assertThrows(ExecutionException.class, () -> servicio.buscar("Managua", "Nicaragua", null).get(3, TimeUnit.SECONDS));
    }

    @Test void aplicaUnSoloLimiteParaBusquedaReferenciaYPais() throws Exception {
        servicio.close();
        servicio = cliente(Duration.ofMillis(100));
        servicio.buscar("warmup", "Nicaragua", null).get(3, TimeUnit.SECONDS);
        tiempos.clear();
        var buscar = servicio.buscar("Managua", "Nicaragua", null);
        var referencia = servicio.referencia(12, -86);
        var pais = servicio.limitesPais("España");
        var otra = servicio.buscar("León", "Nicaragua", null);
        buscar.get(3, TimeUnit.SECONDS);
        referencia.get(3, TimeUnit.SECONDS);
        pais.get(3, TimeUnit.SECONDS);
        otra.get(3, TimeUnit.SECONDS);
        assertEquals(4, tiempos.size());
        assertTrue(Duration.ofNanos(tiempos.get(3) - tiempos.get(0)).toMillis() >= 240,
                "Las distintas operaciones no deben salir como una ráfaga.");
    }

    @Test void noEnviaConsultasCanceladasQueTodaviaEstanEnCola() throws Exception {
        servicio.close();
        servicio = cliente(Duration.ofMillis(100));
        servicio.buscar("warmup", "Nicaragua", null).get(3, TimeUnit.SECONDS);
        consultas.clear();
        var buscar = servicio.buscar("Managua", "Nicaragua", null);
        var cancelada = servicio.referencia(12, -86);
        assertTrue(cancelada.cancel(false));
        var pais = servicio.limitesPais("Nicaragua");
        buscar.get(3, TimeUnit.SECONDS);
        pais.get(3, TimeUnit.SECONDS);
        assertEquals(2, consultas.size());
        assertTrue(consultas.stream().noneMatch(q -> q.startsWith("/reverse")));
    }

    @Test void coordenadasInvalidasNoGeneranPeticiones() {
        assertThrows(ExecutionException.class, () -> servicio.referencia(Double.NaN, 0).get(3, TimeUnit.SECONDS));
        assertThrows(ExecutionException.class, () -> servicio.referencia(91, 0).get(3, TimeUnit.SECONDS));
        assertThrows(ExecutionException.class, () -> servicio.referencia(0, 181).get(3, TimeUnit.SECONDS));
        assertTrue(consultas.isEmpty());
    }
}
