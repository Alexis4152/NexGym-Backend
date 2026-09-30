package com.nexora.sport.dto;

import java.math.BigDecimal;
import java.util.Set;

public record ArticuloInventarioDto(
        Long id,
        Set<Long> categoriaIds,
        Set<String> categoriaNombres,
        String nombre,
        String tipo,
        String codigoBarras,
        int stock,
        int stockMinimo,
        BigDecimal costo,
        BigDecimal precioVenta,
        boolean vendible,
        String imagenUrl,
        boolean reservable,
        BigDecimal descuentoApartadoPorcentaje,
        boolean activo,
        Set<Long> disciplinaIds,
        Set<String> disciplinaNombres
) {}
