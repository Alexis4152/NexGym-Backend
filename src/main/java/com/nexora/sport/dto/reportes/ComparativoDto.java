package com.nexora.sport.dto.reportes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Patron reutilizable "periodo actual vs. periodo anterior" (seccion 4.10 del encargo):
 * ingresos, alumnos, renovaciones, asistencias, etc. pueden compararse contra el rango
 * inmediatamente anterior de la misma duracion. cambioPorcentual es null si el periodo
 * anterior no tuvo actividad (dividir entre cero no tiene sentido para mostrarse).
 */
public record ComparativoDto(
        BigDecimal actual, BigDecimal anterior, BigDecimal cambioPorcentual,
        LocalDate desde, LocalDate hasta, LocalDate desdeAnterior, LocalDate hastaAnterior
) {
    public static ComparativoDto de(BigDecimal actual, BigDecimal anterior, LocalDate desde, LocalDate hasta) {
        long dias = hasta.toEpochDay() - desde.toEpochDay();
        LocalDate desdeAnterior = desde.minusDays(dias + 1);
        LocalDate hastaAnterior = desde.minusDays(1);
        BigDecimal cambio = (anterior != null && anterior.signum() > 0)
                ? actual.subtract(anterior).divide(anterior, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : null;
        return new ComparativoDto(actual, anterior, cambio, desde, hasta, desdeAnterior, hastaAnterior);
    }

    public static ComparativoDto deConteo(long actual, long anterior, LocalDate desde, LocalDate hasta) {
        return de(BigDecimal.valueOf(actual), BigDecimal.valueOf(anterior), desde, hasta);
    }
}
