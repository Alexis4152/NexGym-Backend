package com.nexora.sport.dto.reportes;

import java.time.LocalDate;

public record RiesgoAbandonoItemDto(
        Long alumnoId, String alumnoNombre, String planNombre, String disciplinas,
        LocalDate ultimaAsistencia, long diasSinAsistir, LocalDate fechaFin
) {}
