package com.fleetroute.api.controller;

import com.fleetroute.api.dto.ConfigResponse;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ConfigController {

    private final JdbcTemplate jdbc;

    public ConfigController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/config")
    public ConfigResponse config() {
        String empresa = "FleetRoute Pro";
        try {
            String nombre = jdbc.queryForObject(
                    "select nombre from empresa order by id_empresa limit 1", String.class);
            if (nombre != null && !nombre.isBlank()) {
                empresa = nombre;
            }
        } catch (DataAccessException ignored) {
            // El login mostrará el nombre genérico si la base no está disponible.
        }
        return new ConfigResponse(empresa);
    }
}
