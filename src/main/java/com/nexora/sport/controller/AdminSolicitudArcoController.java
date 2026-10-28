package com.nexora.sport.controller;

import com.nexora.sport.dto.ApiResponse;
import com.nexora.sport.dto.arco.ArcoDtos;
import com.nexora.sport.dto.arco.SolicitudArcoDto;
import com.nexora.sport.model.EstadoSolicitudArco;
import com.nexora.sport.model.Usuario;
import com.nexora.sport.service.SolicitudArcoService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Atencion de todas las solicitudes ARCO: obligacion legal de la plataforma, solo SUPER_ADMIN. */
@RestController
@RequestMapping("/api/admin/arco")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminSolicitudArcoController {

    private final SolicitudArcoService service;

    public AdminSolicitudArcoController(SolicitudArcoService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<SolicitudArcoDto>> listar(@RequestParam(required = false) EstadoSolicitudArco estado) {
        return ApiResponse.ok(service.listar(estado));
    }

    @GetMapping("/abiertas")
    public ApiResponse<Long> abiertas() {
        return ApiResponse.ok(service.contarAbiertas());
    }

    @GetMapping("/{id}")
    public ApiResponse<SolicitudArcoDto> obtener(@PathVariable Long id) {
        return ApiResponse.ok(service.obtener(id));
    }

    @PostMapping("/mostrador")
    public ApiResponse<SolicitudArcoDto> crearEnMostrador(@Valid @RequestBody ArcoDtos.MostradorRequest request,
                                                          @AuthenticationPrincipal Usuario actor) {
        return ApiResponse.ok("Solicitud registrada", service.crearEnMostrador(request, actor));
    }

    @PostMapping("/{id}/verificar-identidad")
    public ApiResponse<SolicitudArcoDto> verificarIdentidad(@PathVariable Long id,
                                                            @Valid @RequestBody ArcoDtos.VerificarIdentidadRequest request,
                                                            @AuthenticationPrincipal Usuario actor) {
        return ApiResponse.ok("Identidad verificada", service.verificarIdentidad(id, request, actor));
    }

    @PostMapping("/{id}/pedir-info")
    public ApiResponse<SolicitudArcoDto> pedirInfo(@PathVariable Long id, @Valid @RequestBody ArcoDtos.PedirInfoRequest request) {
        return ApiResponse.ok("Se le pidió información al titular", service.pedirInfo(id, request));
    }

    @PostMapping("/{id}/extender")
    public ApiResponse<SolicitudArcoDto> extender(@PathVariable Long id, @Valid @RequestBody ArcoDtos.ExtenderRequest request) {
        return ApiResponse.ok("Plazo extendido", service.extender(id, request));
    }

    @PostMapping("/{id}/responder")
    public ApiResponse<SolicitudArcoDto> responder(@PathVariable Long id, @Valid @RequestBody ArcoDtos.ResponderRequest request,
                                                   @AuthenticationPrincipal Usuario actor) {
        return ApiResponse.ok("Solicitud respondida", service.responder(id, request, actor));
    }

    @PostMapping("/{id}/ejecutar")
    public ApiResponse<SolicitudArcoDto> ejecutar(@PathVariable Long id, @Valid @RequestBody ArcoDtos.EjecutarRequest request,
                                                  @AuthenticationPrincipal Usuario actor) {
        return ApiResponse.ok("Solicitud ejecutada y completada", service.ejecutar(id, request, actor));
    }

    @GetMapping(value = "/{id}/constancia.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> constancia(@PathVariable Long id) {
        return ResponseEntity.ok()
                .header("Content-Disposition", "inline; filename=\"constancia-arco-" + id + ".pdf\"")
                .body(service.constanciaPdf(id));
    }
}
