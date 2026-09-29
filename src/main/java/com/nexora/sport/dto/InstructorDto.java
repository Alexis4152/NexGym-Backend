package com.nexora.sport.dto;

import java.util.Set;

public record InstructorDto(
        Long id,
        Long usuarioId,
        String usuarioNombre,
        String nombre,
        String telefono,
        String email,
        String especialidad,
        String fotoUrl,
        boolean activo,
        Set<Long> disciplinaIds,
        Set<String> disciplinaNombres,
        Set<Long> sucursalIds,
        Set<String> sucursalNombres
) {}
