package com.nexora.sport.dto.publico;

import java.math.BigDecimal;
import java.util.List;

public record PublicArticuloApartadoDto(
        Long id,
        String nombre,
        String categoriaNombre,
        BigDecimal precioVenta,
        BigDecimal descuentoPorcentaje,
        BigDecimal precioConDescuento,
        String imagenUrl,
        /** Hasta 3 fotos del articulo (ver ImagenArticuloService, tope de 3 por articulo). */
        List<String> imagenes,
        int stock
) {}
