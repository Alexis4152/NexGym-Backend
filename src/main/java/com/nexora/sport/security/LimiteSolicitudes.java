package com.nexora.sport.security;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limite en memoria de peticiones por clave (ej. "accion:IP") en una ventana deslizante.
 * Para endpoints publicos sin sesion que mandan correos (formulario ARCO): evita que se
 * usen para inundar de correos o para probar tokens al azar. En memoria por instancia,
 * suficiente con un solo backend (igual que el RateLimitInterceptor de DemoPV).
 */
public class LimiteSolicitudes {

    private final int maximo;
    private final long ventanaMs;
    private final Map<String, Deque<Long>> registros = new ConcurrentHashMap<>();

    public LimiteSolicitudes(int maximo, Duration ventana) {
        this.maximo = maximo;
        this.ventanaMs = ventana.toMillis();
    }

    /** @throws IllegalStateException si la clave ya agoto su cupo en la ventana */
    public void registrar(String clave) {
        long ahora = System.currentTimeMillis();
        Deque<Long> marcas = registros.computeIfAbsent(clave, k -> new ArrayDeque<>());
        synchronized (marcas) {
            while (!marcas.isEmpty() && ahora - marcas.peekFirst() > ventanaMs) {
                marcas.pollFirst();
            }
            if (marcas.size() >= maximo) {
                throw new IllegalStateException("Demasiados intentos. Espera unos minutos e intenta de nuevo.");
            }
            marcas.addLast(ahora);
        }
        if (registros.size() > 10_000) {
            registros.entrySet().removeIf(e -> {
                synchronized (e.getValue()) {
                    return e.getValue().isEmpty() || ahora - e.getValue().peekLast() > ventanaMs;
                }
            });
        }
    }
}
