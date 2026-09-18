package com.nexora.sport.dto;

public record ImagenArticuloDto(
        Long id,
        String ruta,
        boolean esPrincipal,
        int orden
) {}
