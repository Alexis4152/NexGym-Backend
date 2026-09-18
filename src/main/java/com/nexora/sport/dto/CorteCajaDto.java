package com.nexora.sport.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CorteCajaDto(
        Long id,
        Long usuarioId,
        String usuarioNombre,
        String cerradoPorNombre,
        BigDecimal montoInicial,
        BigDecimal montoFinal,
        BigDecimal gastos,
        BigDecimal totalVentas,
        BigDecimal ventasEfectivo,
        BigDecimal ventasTarjeta,
        BigDecimal ventasTransferencia,
        BigDecimal ventasOtro,
        int totalTransacciones,
        int canceladas,
        BigDecimal totalCancelado,
        String estado,
        String notas,
        LocalDateTime abiertoEn,
        LocalDateTime cerradoEn
) {}
