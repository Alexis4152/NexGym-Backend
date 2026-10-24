package com.nexora.sport.dto;

public record SucursalDto(
        Long id,
        String nombre,
        /** Texto compuesto (para mostrar); null en sucursales sin domicilio capturado. */
        String direccion,
        /** Domicilio estructurado; null en sucursales anteriores que solo tenian `direccion`. */
        DomicilioDto domicilio,
        String notas,
        boolean activo
) {}
