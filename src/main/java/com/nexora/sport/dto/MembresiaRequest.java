package com.nexora.sport.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MembresiaRequest(
        @NotNull Long alumnoId,
        @NotNull Long planId,
        LocalDate fechaInicio,
        BigDecimal precioFinal,
        /** true = registra automaticamente el ingreso en Caja */
        boolean registrarCobro,
        String metodoPago
) {}
