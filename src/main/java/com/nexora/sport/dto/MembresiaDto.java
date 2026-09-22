package com.nexora.sport.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public record MembresiaDto(
        Long id,
        Long alumnoId,
        String alumnoNombre,
        Long planId,
        String planNombre,
        String tipoPlanSnapshot,
        Set<Long> disciplinaIds,
        Set<String> disciplinaNombres,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        Integer numeroClasesContratadas,
        Integer clasesRestantes,
        BigDecimal precioOriginal,
        BigDecimal descuento,
        BigDecimal precioFinal,
        BigDecimal totalPagado,
        BigDecimal saldo,
        String estado,
        /** Null cuando la membresia no tiene fecha de vencimiento (POR_CLASES/PASE sin vigencia). */
        Long diasParaVencer,
        /** PROXIMA_A_VENCER, SALDO_PENDIENTE, PAGO_VENCIDO, CLASES_POR_AGOTARSE, AGOTADA (ver MembresiaService#calcularIndicadores). */
        List<String> indicadores,
        Long membresiaAnteriorId,
        boolean renovada,
        String suspendidaMotivo,
        String canceladaMotivo
) {}
