package com.nexora.sport.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record UsuarioRequest(
        Long centroId,
        @NotNull Long rolId,
        @NotBlank String nombre,
        @NotBlank @Email String email,
        /** Sucursal "hogar" (nivel ADMIN/OPERATIVO tipico). Null = todas las sucursales del centro. */
        Long sucursalId,
        /** Sucursales adicionales que el Dueno le asigna (p. ej. un gerente con varias
         * sucursales a su cargo) -- se suman a sucursalId, no lo reemplazan. Null/vacio
         * = ninguna extra. */
        Set<Long> sucursalesAdicionalesIds
) {}
