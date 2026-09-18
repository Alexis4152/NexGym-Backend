package com.nexora.sport.controller;

import com.nexora.sport.dto.*;
import com.nexora.sport.model.Usuario;
import com.nexora.sport.service.CorteCajaService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cortes-caja")
@PreAuthorize("@sectionAccess.check('TIENDA')")
public class CorteCajaController {

    private final CorteCajaService corteCajaService;

    public CorteCajaController(CorteCajaService corteCajaService) {
        this.corteCajaService = corteCajaService;
    }

    @GetMapping("/abierto")
    public ApiResponse<CorteCajaDto> abierto(@AuthenticationPrincipal Usuario actor) {
        return ApiResponse.ok(corteCajaService.abierto(actor));
    }

    @GetMapping
    public ApiResponse<PageResponse<CorteCajaDto>> listar(@AuthenticationPrincipal Usuario actor,
                                                            @RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(corteCajaService.listar(actor, PageRequest.of(page, size)));
    }

    @GetMapping("/{id}")
    public ApiResponse<CorteCajaDto> obtener(@PathVariable Long id) {
        return ApiResponse.ok(corteCajaService.obtener(id));
    }

    @GetMapping("/{id}/resumen")
    public ApiResponse<CorteCajaResumenDto> resumen(@PathVariable Long id) {
        return ApiResponse.ok(corteCajaService.resumen(id));
    }

    @PostMapping("/abrir")
    public ApiResponse<CorteCajaDto> abrir(@AuthenticationPrincipal Usuario actor, @Valid @RequestBody AbrirCorteRequest request) {
        return ApiResponse.ok("Corte abierto", corteCajaService.abrir(actor, request));
    }

    @PostMapping("/{id}/cerrar")
    public ApiResponse<CorteCajaDto> cerrar(@PathVariable Long id, @AuthenticationPrincipal Usuario actor,
                                             @Valid @RequestBody CerrarCorteRequest request) {
        return ApiResponse.ok("Corte cerrado", corteCajaService.cerrar(id, actor, request));
    }
}
