package com.nexora.sport.dto;

import java.time.LocalDateTime;

public record NotificacionDto(
        Long id,
        String tipo,
        String destinatarioTipo,
        Long alumnoId,
        String alumnoNombre,
        Long instructorId,
        String instructorNombre,
        String sucursalNombre,
        String titulo,
        String mensaje,
        String entidadTipo,
        Long entidadId,
        boolean leida,
        LocalDateTime leidaEn,
        LocalDateTime createdAt
) {}
