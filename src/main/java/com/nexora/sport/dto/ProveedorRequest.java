package com.nexora.sport.dto;

import jakarta.validation.constraints.NotBlank;

public record ProveedorRequest(
        @NotBlank String nombre, String contacto, String telefono, String email, String notas
) {}
