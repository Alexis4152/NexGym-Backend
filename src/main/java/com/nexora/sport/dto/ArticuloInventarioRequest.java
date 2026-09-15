package com.nexora.sport.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.Set;

public record ArticuloInventarioRequest(
        Long categoriaId,
        @NotBlank String nombre,
        @NotNull String tipo,
        String codigoBarras,
        int stock,
        int stockMinimo,
        BigDecimal costo,
        BigDecimal precioVenta,
        boolean vendible,
        Set<Long> disciplinaIds
) {}
