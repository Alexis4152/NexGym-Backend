package com.nexora.sport.dto.publico;

/** Una opcion de sucursal donde recoger un producto apartado (seccion 31 del encargo): el
 * cliente elige en cual recogerlo -- las que tienen stock=0 se muestran mostrador/gris en el
 * frontend, no seleccionables. articuloId es el que realmente se manda al crear el apartado. */
public record PublicApartadoSucursalDto(
        Long articuloId,
        Long sucursalId,
        String sucursalNombre,
        /** Para armar el enlace "Ver en el mapa" (Google Maps) en el frontend -- null si la
         * sucursal no tiene direccion capturada, ahi simplemente no se muestra el enlace. */
        String sucursalDireccion,
        int stock
) {}
