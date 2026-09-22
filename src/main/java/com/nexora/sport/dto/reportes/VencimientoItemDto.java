package com.nexora.sport.dto.reportes;

import java.math.BigDecimal;
import java.time.LocalDate;

public record VencimientoItemDto(
        Long membresiaId, Long alumnoId, String alumnoNombre, String planNombre, String disciplinas,
        LocalDate fechaFin, BigDecimal saldo, String estado, LocalDate ultimaAsistencia
) {}
