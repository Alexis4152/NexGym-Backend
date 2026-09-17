package com.nexora.sport.dto;

import java.util.Set;

public record LugarDto(
        Long id,
        Long sucursalId,
        String sucursalNombre,
        String nombre,
        String direccion,
        String notas,
        Integer capacidadMaxima,
        boolean activo,
        Set<Long> disciplinaIds,
        Set<String> disciplinaNombres
) {}
