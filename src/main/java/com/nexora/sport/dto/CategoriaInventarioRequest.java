package com.nexora.sport.dto;

import jakarta.validation.constraints.NotBlank;

public record CategoriaInventarioRequest(@NotBlank String nombre) {}
