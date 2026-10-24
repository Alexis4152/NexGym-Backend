package com.nexora.sport.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Movimiento de efectivo en un corte abierto, o con la caja cerrada (solo RETIRO/FONDO, solo
 * administradores). Campos por tipo:
 *  - GASTO: concepto + categoriaId (categoria de EGRESO del libro de Caja); comprobante opcional.
 *  - RETIRO: destino; personaId opcional (default: quien registra).
 *  - FONDO: concepto opcional.
 *  - PRESTAMO_SALIDA / PRESTAMO_ENTRADA: cajaContraparteId.
 *  - restanteContado: solo con caja cerrada, si el admin conto lo que queda.
 */
public record MovimientoEfectivoRequest(
        @NotNull String tipo,
        BigDecimal monto,
        @Size(max = 150) String concepto,
        Long personaId,
        String destino,
        @Size(max = 100) String referencia,
        String comprobanteUrl,
        Long cajaContraparteId,
        Long categoriaId,
        @PositiveOrZero BigDecimal restanteContado
) {}
