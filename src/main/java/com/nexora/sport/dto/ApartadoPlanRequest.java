package com.nexora.sport.dto;

import jakarta.validation.constraints.*;

/** Datos del cliente obligatorios al apartar un plan (a diferencia del apartado de
 * productos, aqui el correo tambien es requerido: seccion 41 del encargo). */
public record ApartadoPlanRequest(
        @NotNull Long planId,
        @NotBlank @Size(max = 150) String clienteNombre,
        @NotBlank @Size(max = 30) String clienteTelefono,
        @NotBlank @Email @Size(max = 150) String clienteEmail,
        @Size(max = 500) String notas
) {}
