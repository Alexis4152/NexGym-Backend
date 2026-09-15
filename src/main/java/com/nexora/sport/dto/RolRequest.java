package com.nexora.sport.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

public record RolRequest(
        @NotBlank String nombre,
        @NotEmpty Set<String> secciones
) {}
