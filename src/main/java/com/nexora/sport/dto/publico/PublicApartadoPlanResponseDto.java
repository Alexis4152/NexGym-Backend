package com.nexora.sport.dto.publico;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PublicApartadoPlanResponseDto(
        Long id,
        String estado,
        BigDecimal montoAnticipo,
        BigDecimal precioPlan,
        LocalDateTime solicitadoEn
) {}
