package com.nexora.sport.dto;

import java.math.BigDecimal;

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
        boolean activo,
        boolean apartadosActivo,
        int horasApartadoDefault,
        BigDecimal montoMaximoDescuentoApartado,
        BigDecimal porcentajeMaximoDescuentoApartado,
        boolean permitirAccesoConAdeudo,
        int diasInactividadRiesgo
) {}
