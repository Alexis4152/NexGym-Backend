package com.nexora.sport.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Corre cada 5 minutos y vence los apartados ACTIVO cuya fecha limite ya paso, restituyendo el stock. */
@Component
public class ApartadoExpiryJob {

    private final ApartadoService apartadoService;

    public ApartadoExpiryJob(ApartadoService apartadoService) {
        this.apartadoService = apartadoService;
    }

    @Scheduled(fixedRate = 5 * 60_000)
    public void vencerPendientes() {
        apartadoService.vencerPendientes();
    }
}
