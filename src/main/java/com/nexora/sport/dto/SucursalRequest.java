package com.nexora.sport.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SucursalRequest(
        @NotBlank @Size(max = 150) String nombre,
        /** Obligatorio al crear y al editar; `direccion` se compone a partir de el. */
        @NotNull(message = "El domicilio es obligatorio") @Valid DomicilioDto domicilio,
        @Size(max = 300) String notas
) {}
