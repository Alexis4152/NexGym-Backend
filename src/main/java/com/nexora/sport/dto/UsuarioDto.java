package com.nexora.sport.dto;

import java.util.Set;

public record UsuarioDto(
        Long id,
        Long centroId,
        String centroNombre,
        Long sucursalId,
        String sucursalNombre,
        Set<Long> sucursalesAdicionalesIds,
        Set<String> sucursalesAdicionalesNombres,
        boolean tieneVariasSucursales,
        Long rolId,
        String rolNombre,
        String nivel,
        Set<String> secciones,
        Set<String> permisos,
        boolean tieneVariosCentros,
        String nombre,
        String email,
        boolean activo,
        boolean mustChangePassword,
        /** Cuenta de instructor acotada (ver AlcanceInstructorService): el frontend la usa para
         * mostrar "Mis clases" y dejar alumnos en solo lectura. */
        boolean esInstructor
) {}
