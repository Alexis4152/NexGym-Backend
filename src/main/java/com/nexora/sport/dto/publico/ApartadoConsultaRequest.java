package com.nexora.sport.dto.publico;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Folio + telefono para consultar (o auto-cancelar) un apartado desde la tienda publica. El
 * folio solo no basta: el telefono debe coincidir con el que se dejo al solicitarlo. Va por
 * POST (no query params) para no dejar el telefono en logs de servidor/proxy, igual que DemoPV.
 * motivo solo aplica al auto-cancelar (opcional).
 */
public record ApartadoConsultaRequest(
        @NotNull Long folio,
        @NotBlank @Size(max = 30) String telefono,
        @Size(max = 500) String motivo
) {}
