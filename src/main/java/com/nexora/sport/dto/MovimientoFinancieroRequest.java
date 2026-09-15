package com.nexora.sport.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MovimientoFinancieroRequest(
        @NotNull Long categoriaId,
        @NotNull @Positive BigDecimal monto,
        String metodoPago,
        String descripcion,
        LocalDate fecha,
        Long alumnoId,
        Long proveedorId
) {}
