package com.nexora.sport.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record PagoMembresiaRequest(
        @NotNull @Positive BigDecimal monto,
        String metodoPago
) {}
