package com.nexora.sport.dto;

import java.math.BigDecimal;

public record VentaItemDto(
        Long id,
        Long articuloId,
        String articuloNombre,
        BigDecimal precioUnitario,
        int cantidad,
        BigDecimal descuento,
        BigDecimal subtotal
) {}
