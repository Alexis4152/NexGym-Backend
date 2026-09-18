package com.nexora.sport.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record AbrirCorteRequest(
        @NotNull @PositiveOrZero BigDecimal montoInicial,
        String notas
) {}
