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
        String planNombre,
        BigDecimal precioPlan,
        BigDecimal montoAnticipo,
        LocalDateTime solicitadoEn,
        String convertidoPorNombre,
        LocalDateTime convertidoEn,
        String canceladoPorNombre,
        LocalDateTime canceladoEn,
        String motivoCancelacion
) {}
