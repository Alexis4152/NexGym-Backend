package com.nexora.sport.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CentroRequest(
        @NotBlank @Size(max = 150) String nombre,
        String slugPublico,
        Boolean catalogoPublicoActivo,
        String colorPrimario,
        String telefono,
        String emailContacto,
        String direccion,
        Boolean apartadosActivo,
        Integer horasApartadoDefault,
        BigDecimal montoMaximoDescuentoApartado,
        BigDecimal porcentajeMaximoDescuentoApartado
) {}
