package com.nexora.sport.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ApartadoCompleteRequest(
        @NotNull String metodoPago,
        @NotNull String tipoTicket,
        BigDecimal montoRecibido,
        @Email String clienteEmail
) {}
