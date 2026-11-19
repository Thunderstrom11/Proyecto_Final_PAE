package com.fleetroute.api.controller;

import com.fleetroute.api.dto.AlertaRequest;
import com.fleetroute.api.dto.AlertaResponse;
import com.fleetroute.api.dto.PanelResponse;
import com.fleetroute.api.dto.RutaResponse;
import com.fleetroute.api.security.Sesion;
import com.fleetroute.api.security.SesionService;
import com.fleetroute.api.service.AlertaService;
import com.fleetroute.api.service.JornadaApiService;
import com.fleetroute.api.service.OperadorPanelService;
import com.fleetroute.api.service.RutaNavegacionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/operador")
public class OperadorController {

    private final OperadorPanelService panelService;
    private final RutaNavegacionService rutaService;
    private final AlertaService alertaService;
    private final JornadaApiService jornadaService;

    public OperadorController(OperadorPanelService panelService,
                              RutaNavegacionService rutaService,
                              AlertaService alertaService,
                              JornadaApiService jornadaService) {
        this.panelService = panelService;
        this.rutaService = rutaService;
        this.alertaService = alertaService;
        this.jornadaService = jornadaService;
    }

    @GetMapping("/panel")
    public PanelResponse panel(@RequestAttribute(SesionService.ATRIBUTO) Sesion sesion) {
        return panelService.panel(sesion);
    }

    @GetMapping("/ruta")
    public RutaResponse ruta(@RequestAttribute(SesionService.ATRIBUTO) Sesion sesion,
                             @RequestParam(required = false) Double lat,
                             @RequestParam(required = false) Double lon,
                             @RequestParam(required = false) String hasta) {
        return rutaService.ruta(sesion, lat, lon, hasta);
    }

    @PostMapping("/alerta")
    public AlertaResponse alerta(@RequestAttribute(SesionService.ATRIBUTO) Sesion sesion,
                                 @RequestBody(required = false) AlertaRequest solicitud) {
        return alertaService.registrar(sesion, solicitud);
    }

    @PostMapping("/jornada/iniciar")
    public Map<String, String> iniciar(@RequestAttribute(SesionService.ATRIBUTO) Sesion sesion) {
        return Map.of("mensaje", jornadaService.iniciar(sesion));
    }

    @PostMapping("/jornada/pausar")
    public Map<String, String> pausar(@RequestAttribute(SesionService.ATRIBUTO) Sesion sesion) {
        return Map.of("mensaje", jornadaService.pausar(sesion));
    }

    @PostMapping("/jornada/reanudar")
    public Map<String, String> reanudar(@RequestAttribute(SesionService.ATRIBUTO) Sesion sesion) {
        return Map.of("mensaje", jornadaService.reanudar(sesion));
    }

    @PostMapping("/jornada/confirmar")
    public Map<String, String> confirmar(@RequestAttribute(SesionService.ATRIBUTO) Sesion sesion) {
        return Map.of("mensaje", jornadaService.confirmar(sesion));
    }
}
