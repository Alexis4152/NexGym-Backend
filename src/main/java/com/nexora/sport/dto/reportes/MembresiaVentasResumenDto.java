package com.nexora.sport.dto.reportes;

import java.math.BigDecimal;
import java.util.List;

/** "Ventas" de membresias = contrataciones NUEVAS (membresiaAnterior es null) creadas en el periodo. */
public record MembresiaVentasResumenDto(
        long nuevasMembresias, BigDecimal totalContratado, BigDecimal totalCobrado, List<EtiquetaValorDto> porPlan
) {}
