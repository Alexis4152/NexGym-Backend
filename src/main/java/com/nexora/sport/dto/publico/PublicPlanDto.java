package com.nexora.sport.dto.publico;

import java.math.BigDecimal;
import java.util.List;

/** Subconjunto publico de MembresiaPlanDto: solo lo que un cliente necesita para
 * conocer la oferta -- sin id de disciplinas ni el conteo crudo de inscritos. */
public record PublicPlanDto(
        Long id,
        String nombre,
        String tipoPlan,
        Integer duracionCantidad,
        String duracionUnidad,
        Integer numeroClasesIncluidas,
        BigDecimal precio,
        boolean accesoCompleto,
        boolean permiteAbonos,
        Integer limiteAlumnos,
        Integer cupoDisponible,
        List<String> disciplinaNombres
) {}
