package com.nexora.sport.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Corre cada 15 minutos: la granularidad del recordatorio de clase queda atada a este
 * intervalo (puede avisar hasta ~15 min despues de cruzar el umbral configurado por centro).
 */
@Component
public class NotificacionClaseRecordatorioJob {

    private final NotificacionSchedulerService schedulerService;

    public NotificacionClaseRecordatorioJob(NotificacionSchedulerService schedulerService) {
        this.schedulerService = schedulerService;
    }

    @Scheduled(fixedRate = 15 * 60_000)
    public void recordatorios() {
        schedulerService.procesarRecordatoriosClase();
    }
}
