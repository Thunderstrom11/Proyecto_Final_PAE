package com.fleetroute.api.controller;

import com.fleetroute.api.dto.CambioPasswordRequest;
import com.fleetroute.api.dto.LoginRequest;
import com.fleetroute.api.dto.LoginResponse;
import com.fleetroute.api.security.Sesion;
import com.fleetroute.api.security.SesionService;
import com.fleetroute.api.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final SesionService sesiones;

    public AuthController(AuthService authService, SesionService sesiones) {
        this.authService = authService;
        this.sesiones = sesiones;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request.usuario().trim(), request.password());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        sesiones.cerrar(authorization);
    }

    @PostMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cambiarPassword(@Valid @RequestBody CambioPasswordRequest solicitud,
                                @RequestAttribute(SesionService.ATRIBUTO) Sesion sesion) {
        authService.cambiarPassword(sesion, solicitud);
    }
}
