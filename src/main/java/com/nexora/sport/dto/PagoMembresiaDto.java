package com.nexora.sport.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record PagoMembresiaDto(
        Long id,
        Long membresiaId,
        BigDecimal monto,
        String metodoPago,
        LocalDate fecha,
        String estado,
        String registradoPorNombre,
        String motivoCancelacion,
        LocalDateTime canceladoEn,
        String canceladoPorNombre,
        LocalDateTime createdAt
) {}
