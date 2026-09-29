package com.nexora.sport.service;

import com.nexora.sport.model.EstadoEnvio;
import com.nexora.sport.model.NotificacionEnvio;
import com.nexora.sport.repository.NotificacionEnvioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Despacha un {@link NotificacionEnvio} de canal EMAIL en un hilo aparte y registra el
 * resultado real (ENVIADA/FALLIDA), a diferencia de MailService#send que es fire-and-forget
 * sin forma de saber si funciono. Nunca debe propagar una excepcion: quien lo llama (dentro
 * de la misma transaccion que registro un pago/reserva/membresia) ya termino su trabajo.
 */
@Service
public class NotificacionEmailDispatcher {

    private static final Logger log = LoggerFactory.getLogger(NotificacionEmailDispatcher.class);
    private static final int MAX_INTENTOS = 3;

    private final NotificacionEnvioRepository envioRepository;
    private final MailService mailService;

    public NotificacionEmailDispatcher(NotificacionEnvioRepository envioRepository, MailService mailService) {
        this.envioRepository = envioRepository;
        this.mailService = mailService;
    }

    @Async
    @Transactional
    public void despachar(Long envioId) {
        NotificacionEnvio envio = envioRepository.findById(envioId).orElse(null);
        if (envio == null) return;
        intentar(envio);
    }

    /** Usado tambien por NotificacionRetryJob para reintentar envios FALLIDA (seccion 25). */
    @Async
    @Transactional
    public void reintentar(Long envioId) {
        NotificacionEnvio envio = envioRepository.findById(envioId).orElse(null);
        if (envio == null || envio.getEstado() != EstadoEnvio.FALLIDA || envio.getIntentos() >= MAX_INTENTOS) return;
        intentar(envio);
    }

    private void intentar(NotificacionEnvio envio) {
        envio.setIntentos(envio.getIntentos() + 1);
        try {
            mailService.sendSync(envio.getDestino(), envio.getNotificacion().getTitulo(), envio.getNotificacion().getMensaje());
            envio.setEstado(EstadoEnvio.ENVIADA);
            envio.setEnviadoEn(LocalDateTime.now());
            envio.setError(null);
        } catch (Exception e) {
            envio.setEstado(EstadoEnvio.FALLIDA);
            String msg = e.getMessage();
            envio.setError(msg != null && msg.length() > 500 ? msg.substring(0, 500) : msg);
            log.warn("Envio de notificacion #{} fallo (intento {}): {}", envio.getId(), envio.getIntentos(), msg);
        }
        envioRepository.save(envio);
    }
}
