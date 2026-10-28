package com.nexora.sport.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Cada hora: purga solicitudes ARCO sin confirmar y cierra las que no respondieron informacion a tiempo. */
@Component
public class ArcoMantenimientoJob {

    private static final Logger log = LoggerFactory.getLogger(ArcoMantenimientoJob.class);

    private final SolicitudArcoService solicitudArcoService;

    public ArcoMantenimientoJob(SolicitudArcoService solicitudArcoService) {
        this.solicitudArcoService = solicitudArcoService;
    }

    @Scheduled(fixedRate = 60 * 60_000)
    public void ejecutar() {
        int tocadas = solicitudArcoService.mantenimiento();
        if (tocadas > 0) {
            log.info("Mantenimiento de solicitudes ARCO: {} fila(s) afectada(s)", tocadas);
        }
    }
}
