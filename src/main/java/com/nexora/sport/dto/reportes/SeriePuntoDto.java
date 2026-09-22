package com.nexora.sport.dto.reportes;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Un punto de una serie temporal (por dia o por mes, segun el reporte que lo use). */
public record SeriePuntoDto(LocalDate fecha, BigDecimal total, long cantidad) {}
