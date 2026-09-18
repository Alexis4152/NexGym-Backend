package com.nexora.sport.dto;

import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record CerrarCorteRequest(
        @PositiveOrZero BigDecimal gastos,
        String notas
) {}
