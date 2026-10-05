package com.nexora.sport.dto;

/** Resultado de "buscar en otras sucursales" (ver InventarioService#buscarEnOtrasSucursales):
 * de solo lectura -- informa donde si hay existencia, nunca permite venderlo desde ahi. */
public record ArticuloOtraSucursalDto(
        Long articuloId,
        String nombre,
        Long sucursalId,
        String sucursalNombre,
        int stock
) {}
