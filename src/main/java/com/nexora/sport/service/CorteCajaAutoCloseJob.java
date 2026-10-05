package com.nexora.sport.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Corre cada minuto y cierra los cortes de caja que alguien olvido cerrar, una vez pasada la
 * hora que cada centro configuro (Centro#horaCierreAutomaticoCorte) -- seccion 34 del encargo,
 * mismo mecanismo que CashCutAutoCloseJob en DemoPV (poll por minuto contra una hora
 * configurable, en vez de un cron fijo), adaptado a que aqui cada centro tiene su propia hora. */
@Component
public class CorteCajaAutoCloseJob {

    private final CorteCajaService corteCajaService;

    public CorteCajaAutoCloseJob(CorteCajaService corteCajaService) {
        this.corteCajaService = corteCajaService;
    }

    @Scheduled(fixedRate = 60_000)
    public void verificarYCerrar() {
        corteCajaService.ejecutarCierreAutomatico();
    }
}
