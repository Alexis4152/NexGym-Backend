package com.nexora.sport.dto;

public record CentroDto(
        Long id,
        String nombre,
        String slugPublico,
        boolean catalogoPublicoActivo,
        String colorPrimario,
        String logoUrl,
        String telefono,
        String emailContacto,
        String direccion,
        boolean activo
) {}
