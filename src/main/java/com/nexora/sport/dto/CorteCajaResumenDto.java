package com.nexora.sport.dto;

import java.math.BigDecimal;

public record CorteCajaResumenDto(
        BigDecimal montoInicial,
        BigDecimal ventasEfectivo,
        BigDecimal ventasTarjeta,
        BigDecimal ventasTransferencia,
        BigDecimal ventasOtro,
        BigDecimal totalVentas,
        int totalTransacciones,
        int canceladas,
        BigDecimal totalCancelado,
        BigDecimal efectivoEsperado
) {}
