package com.nexora.sport.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Corre una vez al dia y marca VENCIDA cualquier membresia activa que ya paso su fecha_fin. */
@Component
public class MembresiaExpiryJob {

    private final MembresiaService membresiaService;

    public MembresiaExpiryJob(MembresiaService membresiaService) {
        this.membresiaService = membresiaService;
    }

    @Scheduled(cron = "0 5 0 * * *")
    public void marcarVencidas() {
        membresiaService.actualizarVencidas(null);
    }
}
