package com.nexora.sport.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DisciplinaRequest(
        @NotBlank @Size(max = 100) String nombre,
        @Size(max = 500) String descripcion,
        String icono,
        String color,
        @NotNull String modalidad,
        Integer limiteAlumnos,
        boolean requiereInstalacion
) {}
