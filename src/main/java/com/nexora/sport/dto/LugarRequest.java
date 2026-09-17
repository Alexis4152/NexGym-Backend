package com.nexora.sport.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record LugarRequest(
        @NotNull Long sucursalId,
        @NotBlank @Size(max = 150) String nombre,
        @Size(max = 300) String direccion,
        @Size(max = 300) String notas,
        Integer capacidadMaxima,
        Set<Long> disciplinaIds
) {}
