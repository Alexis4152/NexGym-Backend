package com.nexora.sport.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Corre una vez al dia, despues de {@link MembresiaExpiryJob} (00:05): revisa
 * vencimientos proximos/hoy/vencidas y genera los resumenes administrativos.
 */
@Component
public class NotificacionMembresiaJob {

    private final NotificacionSchedulerService schedulerService;

    public NotificacionMembresiaJob(NotificacionSchedulerService schedulerService) {
        this.schedulerService = schedulerService;
    }

    @Scheduled(cron = "0 15 0 * * *")
    public void revisarVencimientos() {
        schedulerService.procesarVencimientosMembresias();
    }
}
