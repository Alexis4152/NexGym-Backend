package com.nexora.sport.dto;

import jakarta.validation.constraints.NotBlank;

public record RechazarMovimientoRequest(@NotBlank String motivo) {}
