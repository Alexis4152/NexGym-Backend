package com.nexora.sport.dto;

/** Respuesta de /api/admin/mail-config: nunca incluye la contrasena, solo si ya hay una guardada. */
public record MailConfigDto(
        boolean enabled,
        String smtpHost,
        Integer smtpPort,
        String smtpUsername,
        boolean passwordConfigured
) {}
