package com.nexora.sport.dto;

import jakarta.validation.constraints.NotNull;

public record AjusteStockRequest(
        @NotNull Integer delta,
        String razon
) {}
