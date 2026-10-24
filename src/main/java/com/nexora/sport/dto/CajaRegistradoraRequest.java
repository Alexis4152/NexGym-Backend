package com.nexora.sport.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Alta/edicion de una caja. sucursalId solo se usa al crear (una caja no cambia de sucursal). */
public record CajaRegistradoraRequest(
        @NotBlank @Size(max = 60) String nombre,
        Long sucursalId,
        Boolean activo
) {}
