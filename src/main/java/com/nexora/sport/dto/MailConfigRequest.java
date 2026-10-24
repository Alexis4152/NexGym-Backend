package com.nexora.sport.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * PUT /api/admin/mail-config (solo SUPER_ADMIN). smtpPassword es de solo escritura: null o
 * vacio = conservar la que ya esta guardada, asi el formulario nunca necesita conocerla.
 */
public record MailConfigRequest(
        @NotNull Boolean enabled,
        @NotBlank String smtpHost,
        @NotNull @Min(1) @Max(65535) Integer smtpPort,
        @NotBlank @Email String smtpUsername,
        String smtpPassword
) {}
