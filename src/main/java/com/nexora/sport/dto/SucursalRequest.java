package com.nexora.sport.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SucursalRequest(
        @NotBlank @Size(max = 150) String nombre,
        @Size(max = 300) String direccion,
        @Size(max = 300) String notas
) {}
