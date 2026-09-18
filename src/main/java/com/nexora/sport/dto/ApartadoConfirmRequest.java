package com.nexora.sport.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.List;

public record ApartadoConfirmRequest(
        Integer horasVigencia,
        List<DescuentoItem> descuentos
) {
    public record DescuentoItem(@NotNull Long articuloId, @NotNull @PositiveOrZero BigDecimal descuento) {}
}
