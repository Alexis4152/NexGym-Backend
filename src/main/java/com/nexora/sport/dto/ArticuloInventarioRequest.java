package com.nexora.sport.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.Set;

public record ArticuloInventarioRequest(
        /** Solo tiene efecto para un actor sin sucursal activa fija (Dueno/SUPER_ADMIN en
         * vista "todas"): para un Encargado/Recepcion se ignora y se usa SIEMPRE su propia
         * sucursal activa (ver InventarioService#crear) -- nunca se confia en que el cliente
         * mande la sucursal correcta para un actor ya restringido. */
        Long sucursalId,
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
