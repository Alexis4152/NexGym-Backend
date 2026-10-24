package com.nexora.sport.dto;

import java.math.BigDecimal;

/**
 * Resumen en vivo de un corte (abierto o cerrado), ver CalculadoraCorte. efectivoEsperado =
 * fondo + ventas en efectivo + membresias en efectivo + entradas - gastos - retiros -
 * reembolsos - prestamos a otras cajas.
 *
 * Conteo ciego (Centro#conteoCiego) con el corte abierto y un cajero consultando: llegan en
 * null efectivoEsperado y todo lo que permitiria despejarlo (ventas/membresias en efectivo y
 * los totales de ventas y membresias).
 */
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
        BigDecimal membresiasEfectivo,
        BigDecimal membresiasTarjeta,
        BigDecimal membresiasTransferencia,
        BigDecimal membresiasOtro,
        BigDecimal totalMembresias,
        int pagosMembresia,
        BigDecimal totalEntradas,
        BigDecimal totalGastos,
        BigDecimal totalRetiros,
        BigDecimal totalReembolsos,
        BigDecimal totalPrestamosSalida,
        BigDecimal efectivoEsperado,
        boolean conteoCiego
) {}
