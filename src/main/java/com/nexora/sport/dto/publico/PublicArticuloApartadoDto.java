package com.nexora.sport.dto.publico;

import java.math.BigDecimal;
import java.util.List;

public record PublicArticuloApartadoDto(
        Long id,
        String nombre,
        String categoriaNombre,
        /** Nombres de categoria por separado (a diferencia de categoriaNombre, que va unido
         * por comas): permite que la tienda publica arme los chips de filtro y empareje un
         * articulo con VARIAS categorias a la vez. */
        List<String> categoriaNombres,
        BigDecimal precioVenta,
        BigDecimal descuentoPorcentaje,
        BigDecimal precioConDescuento,
        String imagenUrl,
        /** Hasta 3 fotos del articulo (ver ImagenArticuloService, tope de 3 por articulo). */
        List<String> imagenes,
        int stock
) {}
