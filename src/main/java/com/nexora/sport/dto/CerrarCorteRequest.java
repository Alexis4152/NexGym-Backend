package com.nexora.sport.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Cerrar un corte con ARQUEO: montoContado es el efectivo que hay de verdad en la caja (queda
 * en ella para el siguiente turno). Si no cuadra con el esperado, la nota es obligatoria
 * (salvo conteo ciego). Los gastos ya no se capturan aqui: son movimientos del turno.
 */
public record CerrarCorteRequest(
        @NotNull @PositiveOrZero BigDecimal montoContado,
        @Size(max = 300) String notas,
        Boolean confirmacionDiferenciaGrande
) {}
