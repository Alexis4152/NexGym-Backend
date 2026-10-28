package com.nexora.sport.controller;

import com.nexora.sport.dto.ApiResponse;
import com.nexora.sport.dto.arco.ArcoDtos;
import com.nexora.sport.dto.arco.SolicitudArcoDto;
import com.nexora.sport.model.Usuario;
import com.nexora.sport.service.SolicitudArcoService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * "Mis solicitudes ARCO": cualquier usuario con sesion, sin seccion ni permiso, porque no es
 * una funcion del centro sino del titular viendo y levantando las suyas.
 */
@RestController
@RequestMapping("/api/arco/mis-solicitudes")
public class SolicitudArcoController {

    private final SolicitudArcoService service;

    public SolicitudArcoController(SolicitudArcoService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<SolicitudArcoDto>> listar(@AuthenticationPrincipal Usuario actor) {
        return ApiResponse.ok(service.listarPropias(actor));
    }

    @PostMapping
    public ApiResponse<Void> crear(@Valid @RequestBody ArcoDtos.CrearRequest request, @AuthenticationPrincipal Usuario actor) {
        service.crearPropia(request, actor);
        return ApiResponse.ok("Tu solicitud fue enviada.", null);
    }

    @PostMapping("/{id}/responder-info")
    public ApiResponse<SolicitudArcoDto> responderInfo(@PathVariable Long id,
                                                       @Valid @RequestBody ArcoDtos.ResponderInfoRequest request,
                                                       @AuthenticationPrincipal Usuario actor) {
        return ApiResponse.ok("Tu respuesta fue enviada.", service.responderInfoPropia(id, request, actor));
    }
}
