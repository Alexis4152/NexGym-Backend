package com.nexora.sport.dto.publico;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PublicApartadoResponseDto(
        Long id,
        String estado,
        BigDecimal total,
        int horasVigenciaEstimado,
        LocalDateTime solicitadoEn
) {}
