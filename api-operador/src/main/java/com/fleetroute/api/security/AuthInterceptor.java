package com.fleetroute.api.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final String MENSAJE_SIN_SESION =
            "{\"mensaje\":\"La sesión no es válida o expiró. Inicie sesión nuevamente.\"}";

    private final SesionService sesiones;

    public AuthInterceptor(SesionService sesiones) {
        this.sesiones = sesiones;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        Optional<Sesion> sesion = sesiones.buscar(request.getHeader("Authorization"));
        if (sesion.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(MENSAJE_SIN_SESION);
            return false;
        }
        request.setAttribute(SesionService.ATRIBUTO, sesion.get());
        return true;
    }
}
