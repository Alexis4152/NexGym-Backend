package com.nexora.sport.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Cierre forzado por un administrador (p. ej. el cajero se fue sin cerrar), indicando como se
 * conto el efectivo (homologado con DemoPV#CashCutForceCloseRequest):
 *  - ADMIN_PRESENCIAL: lo conto el admin (montoContado obligatorio).
 *  - REPORTADO_REMOTO: lo reporto el cajero (montoContado + reportadoPorId; evidencia opcional).
 *  - NO_CONTADO: nadie conto, se entrega el esperado.
 */
public record ForzarCierreCorteRequest(
        @NotNull String metodoConteo,
        @PositiveOrZero BigDecimal montoContado,
        Long reportadoPorId,
        String evidenciaUrl,
        @NotBlank @Size(max = 200) String motivo
) {}
