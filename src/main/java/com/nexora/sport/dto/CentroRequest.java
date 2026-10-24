package com.nexora.sport.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalTime;

public record CentroRequest(
        @NotBlank @Size(max = 150) String nombre,
        String slugPublico,
        Boolean catalogoPublicoActivo,
        String colorPrimario,
        String telefono,
        String emailContacto,
        /** Obligatorio al guardar la configuracion del centro (CentroService#actualizar); al
         * crear el centro (SUPER_ADMIN, solo con nombre) aun no se pide. */
        @Valid DomicilioDto domicilio,
        Boolean apartadosActivo,
        Integer horasApartadoDefault,
        BigDecimal montoMaximoDescuentoApartado,
        BigDecimal porcentajeMaximoDescuentoApartado,
        Boolean permitirAccesoConAdeudo,
        Integer diasInactividadRiesgo,
        Boolean notificacionesMembresiaActivo,
        Boolean notificacionesClaseActivo,
        Boolean notificacionesEmailActivo,
        Boolean notificacionesInternoActivo,
        String notificacionesDiasAntesVencimiento,
        Integer notificacionesHorasAntesClase,
        LocalTime horaCierreAutomaticoCorte,
        Boolean conteoCiego
) {}
