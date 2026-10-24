package com.nexora.sport.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CorteCajaDto(
        Long id,
        Long usuarioId,
        String usuarioNombre,
        String sucursalNombre,
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
        LocalDateTime cerradoEn,
        // ---- caja y entrega entre turnos (null en cortes anteriores a las cajas)
        Long cajaId,
        String cajaNombre,
        Long corteAnteriorId,
        String corteAnteriorUsuarioNombre,
        BigDecimal montoInicialEsperado,
        String metodoConteoRecibido,
        String notaRecepcion,
        String estadoEntrega,
        BigDecimal diferenciaEntrega,
        // ---- totales de membresias y movimientos
        BigDecimal membresiasEfectivo,
        BigDecimal membresiasTarjeta,
        BigDecimal membresiasTransferencia,
        BigDecimal membresiasOtro,
        Integer pagosMembresia,
        BigDecimal totalEntradas,
        BigDecimal totalRetiros,
        BigDecimal totalReembolsos,
        BigDecimal totalPrestamosSalida,
        // ---- arqueo de cierre
        BigDecimal montoContado,
        BigDecimal diferenciaEfectivo,
        String metodoConteo,
        String contadoPorNombre,
        String evidenciaConteoUrl,
        String estadoArqueo,
        BigDecimal montoEntregado
) {}
