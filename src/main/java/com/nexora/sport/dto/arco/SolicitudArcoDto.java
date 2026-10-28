package com.nexora.sport.dto.arco;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Vista de una solicitud ARCO. Nunca incluye el token de seguimiento. */
public record SolicitudArcoDto(
        Long id,
        String nombre,
        String email,
        String telefono,
        String tipo,
        String estado,
        String canal,
        String detalle,
        boolean representante,
        String representadoNombre,
        LocalDateTime createdAt,
        LocalDateTime recibidaEn,
        LocalDate fechaLimite,
        LocalDateTime identidadVerificadaEn,
        String identidadMetodo,
        String identidadNotas,
        String infoSolicitada,
        LocalDate infoFechaLimite,
        String infoRespuesta,
        boolean extendida,
        String motivoExtension,
        String resultado,
        String fundamento,
        String respuesta,
        LocalDateTime respondidaEn,
        String respondidaPorNombre,
        LocalDate ejecucionFechaLimite,
        LocalDateTime ejecutadaEn,
        String ejecutadaPorNombre,
        String resumenEjecucion,
        String registradaPorNombre
) {}
