package edu.ni.uam.proyecto_final_pae.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** Búsqueda compartida: HTTP y JSON fuera del hilo de JavaFX, como máximo una petición por segundo. */
public final class NominatimService implements AutoCloseable {
    private static final NominatimService COMPARTIDO = new NominatimService(
            URI.create("https://nominatim.openstreetmap.org"), Duration.ofSeconds(1));
    private static final String USER_AGENT = "FleetRoutePro/1.0 (+https://github.com/Thunderstrom11/Proyecto_Final_PAE)";
    private final URI base;
    private final long intervalo;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final ObjectMapper json = new ObjectMapper();
    private final ScheduledExecutorService ejecutor = Executors.newSingleThreadScheduledExecutor(tarea -> {
        Thread hilo = new Thread(tarea, "nominatim-consultas");
        hilo.setDaemon(true);
        return hilo;
    });
    private final ArrayDeque<Consulta> pendientes = new ArrayDeque<>();
    private boolean trabajando, cerrado, enviada;
    private long ultimoInicio;
    private record Consulta(HttpRequest peticion, boolean lista, CompletableFuture<JsonNode> resultado) {}

    public static NominatimService compartido() { return COMPARTIDO; }

    public NominatimService(URI base, Duration intervalo) {
        if (intervalo.isNegative()) throw new IllegalArgumentException("El intervalo no puede ser negativo.");
        this.base = base;
        this.intervalo = intervalo.toNanos();
    }

    public CompletableFuture<JsonNode> buscar(String texto, String pais, double[] limites) {
        String ruta = "/search?format=jsonv2&addressdetails=1&limit=8&accept-language=es&q="
                + codificar(texto + ", " + pais);
        if (limites != null) ruta += "&viewbox=" + limites[1] + "," + limites[2] + ","
                + limites[3] + "," + limites[0] + "&bounded=1";
        return consultar(ruta, true);
    }

    public CompletableFuture<JsonNode> limitesPais(String pais) {
        return consultar("/search?format=jsonv2&limit=1&featuretype=country&accept-language=es&q=" + codificar(pais), true);
    }

    public CompletableFuture<JsonNode> referencia(double latitud, double longitud) {
        if (!Double.isFinite(latitud) || !Double.isFinite(longitud) || Math.abs(latitud) > 90 || Math.abs(longitud) > 180) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Las coordenadas están fuera de rango."));
        }
        return consultar("/reverse?format=jsonv2&accept-language=es&lat=" + latitud + "&lon=" + longitud, false);
    }

    private static String codificar(String texto) { return URLEncoder.encode(texto, StandardCharsets.UTF_8); }

    private synchronized CompletableFuture<JsonNode> consultar(String ruta, boolean lista) {
        if (cerrado) return CompletableFuture.failedFuture(new IllegalStateException("El buscador está cerrado."));
        var resultado = new CompletableFuture<JsonNode>();
        HttpRequest peticion = HttpRequest.newBuilder(base.resolve(ruta)).header("User-Agent", USER_AGENT)
                .timeout(Duration.ofSeconds(15)).GET().build();
        pendientes.add(new Consulta(peticion, lista, resultado));
        if (!trabajando) {
            trabajando = true;
            ejecutor.execute(this::despachar);
        }
        return resultado;
    }

    private void despachar() {
        Consulta consulta;
        synchronized (this) {
            if (cerrado) return;
            while (!pendientes.isEmpty() && pendientes.peek().resultado().isCancelled()) pendientes.remove();
            if (pendientes.isEmpty()) { trabajando = false; return; }
            long espera = enviada ? intervalo - (System.nanoTime() - ultimoInicio) : 0;
            if (espera > 0) { ejecutor.schedule(this::despachar, espera, TimeUnit.NANOSECONDS); return; }
            consulta = pendientes.remove();
            enviada = true;
            ultimoInicio = System.nanoTime();
        }
        try {
            http.sendAsync(consulta.peticion(), HttpResponse.BodyHandlers.ofString())
                    .whenComplete((respuesta, error) -> {
                        if (error != null) { consulta.resultado().completeExceptionally(error); return; }
                        try {
                            if (respuesta.statusCode() != 200) throw new IOException("El buscador respondió HTTP " + respuesta.statusCode() + ".");
                            JsonNode raiz = json.readTree(respuesta.body());
                            if (raiz == null || (consulta.lista() ? !raiz.isArray() : !raiz.isObject()) || raiz.hasNonNull("error")) {
                                throw new IOException("El buscador devolvió una respuesta inválida.");
                            }
                            consulta.resultado().complete(raiz);
                        } catch (Exception e) { consulta.resultado().completeExceptionally(e); }
                    });
        } catch (RuntimeException e) { consulta.resultado().completeExceptionally(e); }
        synchronized (this) {
            if (!cerrado) ejecutor.execute(this::despachar);
        }
    }

    @Override public synchronized void close() {
        cerrado = true;
        for (Consulta consulta : pendientes) consulta.resultado().cancel(false);
        pendientes.clear();
        ejecutor.shutdownNow();
    }
}
