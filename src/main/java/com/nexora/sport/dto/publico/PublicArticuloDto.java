package com.nexora.sport.dto.publico;

import java.math.BigDecimal;
import java.util.List;

public record PublicArticuloDto(
        Long id, String nombre, String categoriaNombre, BigDecimal precioVenta,
        String imagenUrl,
        /** Hasta 3 fotos del articulo (ver ImagenArticuloService, tope de 3 por articulo);
         * imagenUrl es la primera (portada), para clientes viejos del frontend. */
        List<String> imagenes
) {}
