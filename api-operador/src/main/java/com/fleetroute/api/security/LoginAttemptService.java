package com.fleetroute.api.security;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginAttemptService {

    private static final int MAX_FALLOS = 5;
    private static final Duration VENTANA = Duration.ofMinutes(10);
    private static final Duration BLOQUEO = Duration.ofMinutes(10);

    private final Map<String, Deque<Instant>> fallos = new ConcurrentHashMap<>();
    private final Map<String, Instant> bloqueos = new ConcurrentHashMap<>();

    public long segundosBloqueo(String clave) {
        Instant hasta = bloqueos.get(clave);
        if (hasta == null) {
            return 0;
        }
        Instant ahora = Instant.now();
        if (hasta.isAfter(ahora)) {
            return Math.max(1, Duration.between(ahora, hasta).toSeconds() + 1);
        }
        bloqueos.remove(clave);
        fallos.remove(clave);
        return 0;
    }

    public void registrarFallo(String clave) {
        Instant ahora = Instant.now();
        Deque<Instant> recientes = fallos.computeIfAbsent(clave, k -> new ArrayDeque<>());
        synchronized (recientes) {
            while (!recientes.isEmpty() && recientes.peekFirst().isBefore(ahora.minus(VENTANA))) {
                recientes.pollFirst();
            }
            recientes.addLast(ahora);
            if (recientes.size() >= MAX_FALLOS) {
                bloqueos.put(clave, ahora.plus(BLOQUEO));
                recientes.clear();
            }
        }
    }

    public void limpiar(String clave) {
        fallos.remove(clave);
        bloqueos.remove(clave);
    }
}
