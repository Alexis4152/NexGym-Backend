package com.nexora.sport.dto.publico;

import java.math.BigDecimal;
import java.util.List;

/** Un "producto" de la tienda publica de apartados, agrupado por nombre (seccion 31 del
 * encargo): cada sucursal tiene su propio ArticuloInventario independiente, pero el cliente
 * ve UNA tarjeta por nombre y elige en que sucursal recogerlo -- ver {@link #sucursales}. */
public record PublicArticuloApartadoDto(
        /** Id del articulo representativo (el primero con stock, o el primero si ninguno
         * tiene): solo sirve de key en el frontend, NUNCA se manda al crear un apartado --
         * para eso se usa el articuloId de la sucursal elegida en {@link #sucursales}. */
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
        /** Una entrada por sucursal donde existe este producto (incluye las de stock=0, para
         * que el frontend las muestre en gris con "Sin stock disponible" en vez de ocultarlas). */
        List<PublicApartadoSucursalDto> sucursales
) {}
