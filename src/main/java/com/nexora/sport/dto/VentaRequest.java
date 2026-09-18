package com.nexora.sport.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

public record VentaRequest(
        @Size(max = 150) String clienteNombre,
        @Email @Size(max = 150) String clienteEmail,
        @NotNull String metodoPago,
        @NotNull String tipoTicket,
        BigDecimal descuento,
        BigDecimal impuesto,
        BigDecimal montoRecibido,
        @Size(max = 300) String notas,
        @NotEmpty @Valid List<ItemRequest> items
) {
    public record ItemRequest(
            @NotNull Long articuloId,
            @NotNull @Positive Integer cantidad,
            BigDecimal descuento
    ) {}
}
