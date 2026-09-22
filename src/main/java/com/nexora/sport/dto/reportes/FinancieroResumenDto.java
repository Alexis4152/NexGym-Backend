package com.nexora.sport.dto.reportes;

import java.math.BigDecimal;
import java.util.List;

/** Fuente de verdad: MovimientoFinanciero (anulado=false). Ver ReporteFinancieroService. */
public record FinancieroResumenDto(
        BigDecimal ingresos, BigDecimal egresos, BigDecimal resultado,
        List<EtiquetaValorDto> ingresosPorConcepto, List<EtiquetaValorDto> egresosPorConcepto
) {}
