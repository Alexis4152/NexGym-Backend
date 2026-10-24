package com.nexora.sport.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ApartadoPlanDto(
        Long id,
        String clienteNombre,
        String clienteTelefono,
        String clienteEmail,
        String notas,
        String estado,
        Long planId,
        String planNombre,
        BigDecimal precioPlan,
        BigDecimal montoAnticipo,
        LocalDateTime solicitadoEn,
        String convertidoPorNombre,
        LocalDateTime convertidoEn,
        String canceladoPorNombre,
        LocalDateTime canceladoEn,
        String motivoCancelacion,
        /** Alumno/membresia a los que quedo ligado al convertirse (null si sigue PENDIENTE). */
        Long alumnoId,
        String alumnoNombre,
        Long membresiaId
) {}
