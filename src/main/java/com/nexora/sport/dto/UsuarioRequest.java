package com.nexora.sport.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UsuarioRequest(
        Long centroId,
        @NotNull Long rolId,
        @NotBlank String nombre,
        @NotBlank @Email String email
) {}
