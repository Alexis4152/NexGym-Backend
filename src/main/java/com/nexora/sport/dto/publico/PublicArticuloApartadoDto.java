package com.nexora.sport.dto.publico;

import java.math.BigDecimal;

public record PublicArticuloApartadoDto(
        Long id,
        String nombre,
        String categoriaNombre,
        BigDecimal precioVenta,
        BigDecimal descuentoPorcentaje,
        BigDecimal precioConDescuento,
        String imagenUrl,
        int stock
) {}
