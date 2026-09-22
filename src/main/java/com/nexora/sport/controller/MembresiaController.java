package com.nexora.sport.controller;

import com.nexora.sport.dto.*;
import com.nexora.sport.model.Usuario;
import com.nexora.sport.service.MembresiaService;
import com.nexora.sport.service.PagoMembresiaService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/membresias")
@PreAuthorize("@sectionAccess.check('MEMBRESIAS')")
public class MembresiaController {

    private final MembresiaService membresiaService;
    private final PagoMembresiaService pagoMembresiaService;

    public MembresiaController(MembresiaService membresiaService, PagoMembresiaService pagoMembresiaService) {
        this.membresiaService = membresiaService;
        this.pagoMembresiaService = pagoMembresiaService;
    }

    @GetMapping
    public ApiResponse<PageResponse<MembresiaDto>> listar(@AuthenticationPrincipal Usuario actor,
                                                            @RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(membresiaService.listar(actor, PageRequest.of(page, size)));
    }

    @GetMapping("/alumno/{alumnoId}")
    public ApiResponse<PageResponse<MembresiaDto>> listarPorAlumno(@AuthenticationPrincipal Usuario actor, @PathVariable Long alumnoId,
                                                                     @RequestParam(defaultValue = "0") int page,
                                                                     @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(membresiaService.listarPorAlumno(actor, alumnoId, PageRequest.of(page, size)));
    }

    @GetMapping("/alumno/{alumnoId}/vigentes")
    public ApiResponse<List<MembresiaDto>> vigentesPorAlumno(@AuthenticationPrincipal Usuario actor, @PathVariable Long alumnoId) {
        return ApiResponse.ok(membresiaService.vigentesPorAlumno(actor, alumnoId));
    }

    @GetMapping("/proximas-a-vencer")
    public ApiResponse<List<MembresiaDto>> proximasAVencer(@AuthenticationPrincipal Usuario actor,
                                                             @RequestParam(defaultValue = "7") int dias) {
        return ApiResponse.ok(membresiaService.proximasAVencer(actor, dias));
    }

    @PostMapping
    public ApiResponse<MembresiaDto> crear(@AuthenticationPrincipal Usuario actor, @Valid @RequestBody MembresiaRequest request) {
        return ApiResponse.ok("Membresia registrada", membresiaService.crear(actor, request));
    }

    @PostMapping("/{id}/renovar")
    public ApiResponse<MembresiaDto> renovar(@AuthenticationPrincipal Usuario actor, @PathVariable Long id,
                                              @Valid @RequestBody MembresiaRenovarRequest request) {
        return ApiResponse.ok("Membresia renovada", membresiaService.renovar(actor, id, request));
    }

    @PostMapping("/{id}/suspender")
    public ApiResponse<MembresiaDto> suspender(@AuthenticationPrincipal Usuario actor, @PathVariable Long id,
                                                @Valid @RequestBody MembresiaMotivoRequest request) {
        return ApiResponse.ok("Membresia suspendida", membresiaService.suspender(actor, id, request.motivo()));
    }

    @PostMapping("/{id}/reanudar")
    public ApiResponse<MembresiaDto> reanudar(@AuthenticationPrincipal Usuario actor, @PathVariable Long id) {
        return ApiResponse.ok("Membresia reanudada", membresiaService.reanudar(actor, id));
    }

    @PostMapping("/{id}/cancelar")
    public ApiResponse<MembresiaDto> cancelar(@AuthenticationPrincipal Usuario actor, @PathVariable Long id,
                                               @Valid @RequestBody MembresiaMotivoRequest request) {
        return ApiResponse.ok("Membresia cancelada", membresiaService.cancelar(actor, id, request.motivo()));
    }

    @GetMapping("/{id}/pagos")
    public ApiResponse<List<PagoMembresiaDto>> listarPagos(@AuthenticationPrincipal Usuario actor, @PathVariable Long id) {
        return ApiResponse.ok(pagoMembresiaService.listarPorMembresia(actor, id));
    }

    @PostMapping("/{id}/pagos")
    public ApiResponse<PagoMembresiaDto> registrarPago(@AuthenticationPrincipal Usuario actor, @PathVariable Long id,
                                                        @Valid @RequestBody PagoMembresiaRequest request) {
        return ApiResponse.ok("Pago registrado", pagoMembresiaService.registrarPago(actor, id, request));
    }

    @PostMapping("/{id}/pagos/{pagoId}/cancelar")
    public ApiResponse<PagoMembresiaDto> cancelarPago(@AuthenticationPrincipal Usuario actor, @PathVariable Long id,
                                                       @PathVariable Long pagoId, @Valid @RequestBody PagoMembresiaCancelarRequest request) {
        return ApiResponse.ok("Pago cancelado", pagoMembresiaService.cancelarPago(actor, id, pagoId, request.motivo()));
    }
}
