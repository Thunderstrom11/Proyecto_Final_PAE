package com.fleetroute.api.service;

import com.fleetroute.api.config.ApiException;
import com.fleetroute.api.dto.PanelResponse;
import com.fleetroute.api.dto.RutaResponse;
import com.fleetroute.api.security.Sesion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class RutaNavegacionService {

    private static final Logger LOG = LoggerFactory.getLogger(RutaNavegacionService.class);
    private static final String PROVEEDOR = "OpenRouteService";

    private final OperadorPanelService panelService;
    private final String apiKey;
    private final RestClient restClient;

    public RutaNavegacionService(OperadorPanelService panelService,
                                 @Value("${OPENROUTESERVICE_API_KEY:}") String apiKey) {
        this.panelService = panelService;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(6))
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(15));
        this.restClient = RestClient.builder()
                .baseUrl("https://api.openrouteservice.org")
                .requestFactory(factory)
                .build();
    }

    public RutaResponse ruta(Sesion sesion) {
        if (apiKey.isBlank()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                    "La navegación con OpenRouteService no está configurada.");
        }

        PanelResponse panel = panelService.panel(sesion);
        if (!panel.tieneViaje() || panel.viaje() == null) {
            throw new ApiException(HttpStatus.NOT_FOUND,
                    "No hay un viaje activo para calcular la ruta.");
        }

        PanelResponse.Viaje viaje = panel.viaje();
        if (viaje.latitudOrigen() == null || viaje.longitudOrigen() == null
                || viaje.latitudDestino() == null || viaje.longitudDestino() == null) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "El viaje no tiene coordenadas completas de origen y destino.");
        }

        try {
            JsonNode respuesta = restClient.post()
                    .uri("/v2/directions/driving-car/geojson")
                    .header(HttpHeaders.AUTHORIZATION, apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "coordinates", List.of(
                                    List.of(viaje.longitudOrigen(), viaje.latitudOrigen()),
                                    List.of(viaje.longitudDestino(), viaje.latitudDestino())),
                            "instructions", true,
                            "language", "es",
                            "units", "m"))
                    .retrieve()
                    .body(JsonNode.class);
            return mapear(respuesta);
        } catch (RestClientResponseException ex) {
            LOG.warn("OpenRouteService respondió {} al calcular la ruta del viaje {}",
                    ex.getStatusCode(), viaje.id());
            throw new ApiException(HttpStatus.BAD_GATEWAY,
                    "OpenRouteService no pudo calcular la ruta en este momento.");
        } catch (ResourceAccessException ex) {
            LOG.warn("No se pudo conectar con OpenRouteService para el viaje {}", viaje.id(), ex);
            throw new ApiException(HttpStatus.GATEWAY_TIMEOUT,
                    "OpenRouteService tardó demasiado en responder.");
        }
    }

    static RutaResponse mapear(JsonNode respuesta) {
        if (respuesta == null) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "OpenRouteService devolvió una respuesta vacía.");
        }
        JsonNode feature = respuesta.path("features").path(0);
        JsonNode coordenadas = feature.path("geometry").path("coordinates");
        if (!coordenadas.isArray() || coordenadas.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_GATEWAY,
                    "OpenRouteService no devolvió la geometría de la ruta.");
        }

        List<RutaResponse.Coordenada> puntos = new ArrayList<>();
        for (JsonNode par : coordenadas) {
            if (par.isArray() && par.size() >= 2) {
                puntos.add(new RutaResponse.Coordenada(par.get(1).asDouble(), par.get(0).asDouble()));
            }
        }
        if (puntos.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_GATEWAY,
                    "OpenRouteService no devolvió puntos válidos para la ruta.");
        }

        JsonNode properties = feature.path("properties");
        JsonNode summary = properties.path("summary");
        List<RutaResponse.Paso> pasos = new ArrayList<>();
        int indicePaso = 0;

        JsonNode segmentos = properties.path("segments");
        if (segmentos.isArray()) {
            for (JsonNode segmento : segmentos) {
                JsonNode steps = segmento.path("steps");
                if (!steps.isArray()) {
                    continue;
                }
                for (JsonNode step : steps) {
                    JsonNode wayPoints = step.path("way_points");
                    int inicio = wayPoints.isArray() && wayPoints.size() > 0
                            ? wayPoints.get(0).asInt() : 0;
                    int fin = wayPoints.isArray() && wayPoints.size() > 1
                            ? wayPoints.get(1).asInt() : Math.min(inicio + 1, puntos.size() - 1);
                    String instruccion = step.path("instruction").asText("");
                    if (instruccion.startsWith("Camina ")) {
                        instruccion = "Dirígete " + instruccion.substring("Camina ".length());
                    }
                    pasos.add(new RutaResponse.Paso(
                            indicePaso++,
                            inicio,
                            fin,
                            step.path("distance").asDouble(),
                            step.path("duration").asDouble(),
                            instruccion,
                            step.path("name").asText(""),
                            step.path("type").asInt()));
                }
            }
        }

        double distancia = summary.path("distance").asDouble(0);
        double duracion = summary.path("duration").asDouble(0);
        if (distancia <= 0) {
            distancia = pasos.stream().mapToDouble(RutaResponse.Paso::distanciaMetros).sum();
        }
        if (duracion <= 0) {
            duracion = pasos.stream().mapToDouble(RutaResponse.Paso::duracionSegundos).sum();
        }
        return new RutaResponse(PROVEEDOR, distancia, duracion, List.copyOf(puntos), List.copyOf(pasos));
    }
}
