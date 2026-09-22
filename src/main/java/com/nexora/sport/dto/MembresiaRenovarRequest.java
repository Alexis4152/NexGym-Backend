package com.nexora.sport.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

public record MembresiaRenovarRequest(
        /** Mismo plan actual, u otro (cambio de plan al renovar). */
        @NotNull Long planId,
        Set<Long> disciplinaIds,
        /** Solo Dueno/Administrador puede forzarla; el resto siempre usa la fecha auto-calculada (ver seccion 9/10). */
        LocalDate fechaInicio,
        BigDecimal descuento,
        BigDecimal montoPagoInicial,
        String metodoPago
) {}
