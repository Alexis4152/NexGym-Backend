package com.nexora.sport.dto;

import jakarta.validation.constraints.NotBlank;

public record PagoMembresiaCancelarRequest(@NotBlank String motivo) {}
