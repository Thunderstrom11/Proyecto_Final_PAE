package com.fleetroute.api.controller;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class SaludController {

    private final JdbcTemplate jdbc;

    public SaludController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/salud")
    public ResponseEntity<Map<String, Object>> salud() {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("hora", OffsetDateTime.now());
        try {
            jdbc.queryForObject("select 1", Integer.class);
            cuerpo.put("estado", "ok");
            cuerpo.put("baseDatos", "ok");
            return ResponseEntity.ok(cuerpo);
        } catch (DataAccessException ex) {
            cuerpo.put("estado", "degradado");
            cuerpo.put("baseDatos", "error");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(cuerpo);
        }
    }
}
