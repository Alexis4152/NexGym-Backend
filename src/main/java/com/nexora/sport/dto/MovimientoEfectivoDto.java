package com.nexora.sport.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MovimientoEfectivoDto(
        Long id,
        String tipo,
        boolean salida,
        BigDecimal monto,
        String concepto,
        Long personaId,
        String personaNombre,
        String destino,
        String referencia,
        String comprobanteUrl,
        String cajaContraparteNombre,
        Long corteCajaId,
        String registradoPorNombre,
        LocalDateTime createdAt,
        LocalDateTime ocurridoEn,
        boolean registradoDespues,
        BigDecimal restanteEsperado,
        BigDecimal restanteContado,
        Long movimientoFinancieroId
) {}
