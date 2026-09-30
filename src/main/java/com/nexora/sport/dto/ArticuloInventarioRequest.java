package com.nexora.sport.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.Set;

public record ArticuloInventarioRequest(
        Set<Long> categoriaIds,
        @NotBlank String nombre,
        @NotNull String tipo,
        String codigoBarras,
        int stock,
        int stockMinimo,
        BigDecimal costo,
        BigDecimal precioVenta,
        boolean vendible,
        boolean reservable,
        BigDecimal descuentoApartadoPorcentaje,
        Set<Long> disciplinaIds
) {}
