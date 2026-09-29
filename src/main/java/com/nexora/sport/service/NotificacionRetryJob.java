package com.nexora.sport.service;

import com.nexora.sport.model.EstadoEnvio;
import com.nexora.sport.model.NotificacionEnvio;
import com.nexora.sport.repository.NotificacionEnvioRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Reintento sencillo (seccion 25 del encargo): cada 15 min busca envios FALLIDA con menos
 * de 3 intentos y los vuelve a mandar. Sin backoff exponencial ni cola dedicada: no se
 * justifica una infraestructura mas compleja para el volumen de un MVP.
 */
@Component
public class NotificacionRetryJob {

    private static final int MAX_INTENTOS = 3;

    private final NotificacionEnvioRepository envioRepository;
    private final NotificacionEmailDispatcher emailDispatcher;

    public NotificacionRetryJob(NotificacionEnvioRepository envioRepository, NotificacionEmailDispatcher emailDispatcher) {
        this.envioRepository = envioRepository;
        this.emailDispatcher = emailDispatcher;
    }

    @Scheduled(fixedRate = 15 * 60_000)
    public void reintentarFallidos() {
        for (NotificacionEnvio envio : envioRepository.findByEstadoAndIntentosLessThan(EstadoEnvio.FALLIDA, MAX_INTENTOS)) {
            emailDispatcher.reintentar(envio.getId());
        }
    }
}
