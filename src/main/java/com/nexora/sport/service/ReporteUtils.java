package com.nexora.sport.service;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;

/** Conversion de las proyecciones Object[] (JPQL/nativas) usadas por los servicios de Reportes. */
final class ReporteUtils {
    private ReporteUtils() {}

    static LocalDate toLocalDate(Object o) {
        return o instanceof Date d ? d.toLocalDate() : (LocalDate) o;
    }

    static long toLong(Object o) {
        return ((Number) o).longValue();
    }

    static BigDecimal toBigDecimal(Object o) {
        if (o == null) return BigDecimal.ZERO;
        if (o instanceof BigDecimal bd) return bd;
        return BigDecimal.valueOf(((Number) o).doubleValue());
    }
}
