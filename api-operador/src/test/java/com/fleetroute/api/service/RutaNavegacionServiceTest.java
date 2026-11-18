package com.fleetroute.api.service;

import com.fleetroute.api.dto.RutaResponse;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class RutaNavegacionServiceTest {

    @Test
    void convierteLaRespuestaGeoJsonDeOpenRouteService() {
        String json = """
                {
                  "type": "FeatureCollection",
                  "features": [{
                    "type": "Feature",
                    "geometry": {
                      "type": "LineString",
                      "coordinates": [[-86.2514, 12.1364], [-86.5, 12.25], [-86.878, 12.4379]]
                    },
                    "properties": {
                      "summary": { "distance": 12500.5, "duration": 1500.0 },
                      "segments": [{
                        "distance": 12500.5,
                        "duration": 1500.0,
                        "steps": [
                          {
                            "distance": 1500.0,
                            "duration": 180.0,
                            "type": 11,
                            "instruction": "Dirígete al norte por Avenida Central",
                            "name": "Avenida Central",
                            "way_points": [0, 1]
                          },
                          {
                            "distance": 11000.5,
                            "duration": 1320.0,
                            "type": 6,
                            "instruction": "Continúa por Carretera a León",
                            "name": "Carretera a León",
                            "way_points": [1, 2]
                          }
                        ]
                      }]
                    }
                  }]
                }
                """;
        JsonNode respuesta = new ObjectMapper().readTree(json);

        RutaResponse ruta = RutaNavegacionService.mapear(respuesta);

        assertEquals("OpenRouteService", ruta.proveedor());
        assertEquals(3, ruta.puntos().size());
        assertEquals(12.1364, ruta.puntos().get(0).lat());
        assertEquals(-86.2514, ruta.puntos().get(0).lng());
        assertEquals(12500.5, ruta.distanciaMetros());
        assertEquals(2, ruta.pasos().size());
        assertEquals(0, ruta.pasos().get(0).inicio());
        assertEquals(1, ruta.pasos().get(0).fin());
        assertEquals("Continúa por Carretera a León", ruta.pasos().get(1).instruccion());
        assertFalse(ruta.pasos().isEmpty());
    }
}
