package com.nexora.sport.dto.publico;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** "¿No tienes tu folio?": buscar los apartados recientes de un centro solo por telefono. */
public record ApartadoTelefonoRequest(@NotBlank @Size(max = 30) String telefono) {}
