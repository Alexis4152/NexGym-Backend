package com.nexora.sport.dto;

import java.util.Set;

public record UsuarioDto(
        Long id,
        Long centroId,
        String centroNombre,
        Long rolId,
        String rolNombre,
        Set<String> secciones,
        String nombre,
        String email,
        boolean activo,
        boolean mustChangePassword
) {}
