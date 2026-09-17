package com.nexora.sport.dto;

public record SucursalDto(
        Long id,
        String nombre,
        String direccion,
        String notas,
        boolean activo
) {}
