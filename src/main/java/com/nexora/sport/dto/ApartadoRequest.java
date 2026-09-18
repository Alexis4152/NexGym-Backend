package com.nexora.sport.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

public record ApartadoRequest(
        @NotBlank @Size(max = 150) String clienteNombre,
        @NotBlank @Size(max = 30) String clienteTelefono,
        @Email @Size(max = 150) String clienteEmail,
        @Size(max = 500) String notas,
        @NotEmpty @Valid List<ItemRequest> items
) {
    public record ItemRequest(@NotNull Long articuloId, @NotNull @Positive Integer cantidad) {}
}
