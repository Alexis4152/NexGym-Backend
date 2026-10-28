package com.nexora.sport.controller;

import com.nexora.sport.dto.ApiResponse;
import com.nexora.sport.dto.arco.ArcoDtos;
import com.nexora.sport.dto.arco.SolicitudArcoDto;
import com.nexora.sport.security.LimiteSolicitudes;
import com.nexora.sport.service.SolicitudArcoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

/**
 * Formulario publico de derechos ARCO y su seguimiento por link, sin sesion (SecurityConfig
 * deja abierto /api/public/**). No cuelga de un centro: la solicitud es de la plataforma y
 * la atiende el SUPER_ADMIN. Limitado por IP, igual que en DemoPV.
 */
@RestController
@RequestMapping("/api/public/arco")
public class PublicArcoController {

    private static final LimiteSolicitudes LIMITE_ALTA = new LimiteSolicitudes(3, Duration.ofMinutes(15));
    private static final LimiteSolicitudes LIMITE_SEGUIMIENTO = new LimiteSolicitudes(60, Duration.ofMinutes(15));
    private static final LimiteSolicitudes LIMITE_RESPUESTA = new LimiteSolicitudes(15, Duration.ofMinutes(15));

    private final SolicitudArcoService service;

    public PublicArcoController(SolicitudArcoService service) {
        this.service = service;
    }

    @PostMapping
    public ApiResponse<Void> crear(@Valid @RequestBody ArcoDtos.CrearRequest request, HttpServletRequest http) {
        LIMITE_ALTA.registrar(http.getRemoteAddr());
        service.crearPublica(request);
        return ApiResponse.ok("Revisa tu correo para confirmar tu solicitud.", null);
    }

    /** Abrir el link confirma el correo (si hacia falta) y regresa el estado actual. */
    @GetMapping("/seguimiento/{token}")
    public ApiResponse<SolicitudArcoDto> seguimiento(@PathVariable String token, HttpServletRequest http) {
        LIMITE_SEGUIMIENTO.registrar(http.getRemoteAddr());
        return ApiResponse.ok(service.seguimiento(token));
    }

    @PostMapping("/seguimiento/{token}/responder-info")
    public ApiResponse<SolicitudArcoDto> responderInfo(@PathVariable String token,
                                                       @Valid @RequestBody ArcoDtos.ResponderInfoRequest request,
                                                       HttpServletRequest http) {
        LIMITE_RESPUESTA.registrar(http.getRemoteAddr());
        return ApiResponse.ok("Tu respuesta fue enviada.", service.responderInfoPorToken(token, request));
    }
}
