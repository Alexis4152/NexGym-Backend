package com.nexora.sport.dto.reportes;

import java.math.BigDecimal;
import java.util.List;

public record AsistenciasResumenDto(
        long total, long alumnosUnicos, BigDecimal promedioPorAlumno,
        List<SeriePuntoDto> porDia, List<EtiquetaValorDto> porDisciplina, List<EtiquetaValorDto> porSucursal
) {}
