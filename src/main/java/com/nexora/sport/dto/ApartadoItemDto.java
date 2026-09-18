package com.nexora.sport.dto;

import java.math.BigDecimal;

public record ApartadoItemDto(
        Long id,
        Long articuloId,
        String articuloNombre,
        BigDecimal precioUnitario,
        int cantidad,
        BigDecimal descuento,
        BigDecimal subtotal,
        Integer disponible
) {}
