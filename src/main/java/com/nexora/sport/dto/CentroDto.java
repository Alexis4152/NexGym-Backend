package com.nexora.sport.dto;

import java.math.BigDecimal;
import java.time.LocalTime;

public record CentroDto(
        Long id,
        String nombre,
        String slugPublico,
        boolean catalogoPublicoActivo,
        String colorPrimario,
        String logoUrl,
        String telefono,
        String emailContacto,
        String direccion,
        /** null mientras el dueno no capture el domicilio estructurado. */
        DomicilioDto domicilio,
        boolean activo,
        boolean apartadosActivo,
        int horasApartadoDefault,
        BigDecimal montoMaximoDescuentoApartado,
        BigDecimal porcentajeMaximoDescuentoApartado,
        boolean permitirAccesoConAdeudo,
        int diasInactividadRiesgo,
        boolean notificacionesMembresiaActivo,
        boolean notificacionesClaseActivo,
        boolean notificacionesEmailActivo,
        boolean notificacionesInternoActivo,
        String notificacionesDiasAntesVencimiento,
        Integer notificacionesHorasAntesClase,
        LocalTime horaCierreAutomaticoCorte,
        /** Conteo ciego del corte de caja (ver Centro#conteoCiego). */
        boolean conteoCiego
) {}
