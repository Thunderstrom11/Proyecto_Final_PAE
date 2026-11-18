package com.fleetroute.api.security;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SesionService {

    public static final String ATRIBUTO = "sesion";
    private static final Duration DURACION = Duration.ofHours(24);
    private static final String PREFIJO = "Bearer ";

    private final SecureRandom aleatorio = new SecureRandom();
    private final Map<String, Sesion> sesiones = new ConcurrentHashMap<>();

    public Sesion crear(int idUsuario, int idOperador, String username, String nombreOperador,
                        String cedula, String empresa, String zonaHoraria, String moneda) {
        byte[] bytes = new byte[32];
        aleatorio.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Sesion sesion = new Sesion(token, idUsuario, idOperador, username, nombreOperador,
                cedula, empresa, zonaHoraria, moneda, Instant.now().plus(DURACION));
        sesiones.put(token, sesion);
        return sesion;
    }

    public Optional<Sesion> buscar(String authorization) {
        String token = token(authorization);
        if (token == null) {
            return Optional.empty();
        }
        Sesion sesion = sesiones.get(token);
        if (sesion == null) {
            return Optional.empty();
        }
        if (!sesion.expira().isAfter(Instant.now())) {
            sesiones.remove(token);
            return Optional.empty();
        }
        // Sesión deslizante: mientras el operador use la vista, no se cierra por tiempo.
        Sesion renovada = new Sesion(sesion.token(), sesion.idUsuario(), sesion.idOperador(),
                sesion.username(), sesion.nombreOperador(), sesion.cedula(), sesion.empresa(),
                sesion.zonaHoraria(), sesion.moneda(), Instant.now().plus(DURACION));
        sesiones.put(token, renovada);
        return Optional.of(renovada);
    }

    public void cerrar(String authorization) {
        String token = token(authorization);
        if (token != null) {
            sesiones.remove(token);
        }
    }

    private static String token(String authorization) {
        if (authorization == null || authorization.length() <= PREFIJO.length()
                || !authorization.regionMatches(true, 0, PREFIJO, 0, PREFIJO.length())) {
            return null;
        }
        String token = authorization.substring(PREFIJO.length()).trim();
        return token.isEmpty() ? null : token;
    }
}
