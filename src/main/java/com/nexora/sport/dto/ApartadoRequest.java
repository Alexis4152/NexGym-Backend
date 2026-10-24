package com.nexora.sport.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

public record ApartadoRequest(
        @NotBlank @Size(max = 150) String clienteNombre,
        // Celular de 10 digitos sin lada ni separadores, mismo criterio que la tienda publica
        // (PublicApartar.jsx filtra tecla por tecla) y que DemoPV.
        @NotBlank @Pattern(regexp = "\\d{10}", message = "El telefono debe tener 10 digitos") String clienteTelefono,
        @Email @Size(max = 150) String clienteEmail,
        @Size(max = 500) String notas,
        @NotEmpty @Valid List<ItemRequest> items
) {
    public record ItemRequest(@NotNull Long articuloId, @NotNull @Positive Integer cantidad) {}
}
