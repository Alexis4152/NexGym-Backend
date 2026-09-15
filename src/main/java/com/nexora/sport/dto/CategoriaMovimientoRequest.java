package com.nexora.sport.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CategoriaMovimientoRequest(@NotBlank String nombre, @NotNull String tipo) {}
