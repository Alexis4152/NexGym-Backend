package com.nexora.sport.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

public record MembresiaRequest(
        @NotNull Long alumnoId,
        @NotNull Long planId,
        /** Disciplinas elegidas de entre las permitidas por el plan (vacio si el plan no restringe o es accesoCompleto). */
        Set<Long> disciplinaIds,
        /** Por defecto hoy; en una renovacion anticipada/tardia se recalcula (ver MembresiaService#renovar). */
        LocalDate fechaInicio,
        /** Descuento sobre plan.precio; el total contratado (precioFinal) se calcula en el servidor. */
        BigDecimal descuento,
        /** Pago inicial opcional (0/null = queda toda la deuda como saldo, solo permitido si el plan permite abonos). */
        BigDecimal montoPagoInicial,
        String metodoPago
) {}
