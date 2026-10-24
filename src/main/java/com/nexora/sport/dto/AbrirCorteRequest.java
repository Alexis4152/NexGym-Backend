package com.nexora.sport.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Abrir un corte RECIBIENDO una caja (homologado con DemoPV#CashCutOpenRequest): el cajero
 * cuenta el efectivo que hay (montoContado, su fondo inicial) y confirma que esta de acuerdo.
 * Si no coincide con lo que debia recibir, la nota es obligatoria. Con una diferencia grande
 * el backend pide confirmarla (confirmacionDiferenciaGrande = true al reenviar).
 */
public record AbrirCorteRequest(
        @NotNull Long cajaRegistradoraId,
        @NotNull @PositiveOrZero BigDecimal montoContado,
        Boolean deAcuerdo,
        Boolean confirmacionDiferenciaGrande,
        @Size(max = 500) String nota
) {}
