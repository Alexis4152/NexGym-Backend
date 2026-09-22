package com.nexora.sport.dto.reportes;

import java.time.LocalDate;

public record RetencionItemDto(
        Long alumnoId, String alumnoNombre, String planAnteriorNombre, LocalDate fechaFinAnterior,
        boolean renovo, LocalDate fechaNuevaMembresia
) {}
