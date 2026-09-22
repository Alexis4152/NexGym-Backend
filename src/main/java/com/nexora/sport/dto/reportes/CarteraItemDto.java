package com.nexora.sport.dto.reportes;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CarteraItemDto(
        Long membresiaId, Long alumnoId, String alumnoNombre, String planNombre,
        BigDecimal totalContratado, BigDecimal totalPagado, BigDecimal saldo,
        LocalDate fechaFin, String estado
) {}
