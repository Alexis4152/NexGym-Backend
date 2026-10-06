package com.nexora.sport.dto;

import java.math.BigDecimal;
import java.util.Set;

public record ArticuloInventarioDto(
        Long id,
        Long sucursalId,
        String sucursalNombre,
        /** Solo tiene valor en la vista agregada "todas las sucursales" (Dueno/SUPER_ADMIN sin
         * filtrar a una sola): cuantas sucursales se sumaron en esta fila. Null en una fila
         * normal de una sola sucursal -- junto con sucursalId=null es como el frontend
         * distingue una fila agregada (sin acciones de Ajustar/Editar/Desactivar, no hay un
         * solo articulo al que apunten) de una real. */
        Integer sucursalesCount,
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
