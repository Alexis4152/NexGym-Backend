package com.nexora.sport.controller;

import com.nexora.sport.dto.*;
import com.nexora.sport.model.Usuario;
import com.nexora.sport.service.CorteCajaService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

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
                                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                                            @RequestParam(required = false) String estado,
                                                            @RequestParam(required = false) Long usuarioId,
                                                            @RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(corteCajaService.listar(actor, desde, hasta, estado, usuarioId, PageRequest.of(page, size)));
    }

    @GetMapping("/cajeros")
    public ApiResponse<List<CajeroDto>> cajeros(@AuthenticationPrincipal Usuario actor) {
        return ApiResponse.ok(corteCajaService.cajeros(actor));
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
    @PreAuthorize("@sectionAccess.check('TIENDA') and @permisoAccess.check('CAJA_ABRIR')")
    public ApiResponse<CorteCajaDto> abrir(@AuthenticationPrincipal Usuario actor, @Valid @RequestBody AbrirCorteRequest request) {
        return ApiResponse.ok("Corte abierto", corteCajaService.abrir(actor, request));
    }

    @PostMapping("/{id}/cerrar")
    @PreAuthorize("@sectionAccess.check('TIENDA') and @permisoAccess.check('CAJA_CERRAR')")
    public ApiResponse<CorteCajaDto> cerrar(@PathVariable Long id, @AuthenticationPrincipal Usuario actor,
                                             @Valid @RequestBody CerrarCorteRequest request) {
        return ApiResponse.ok("Corte cerrado", corteCajaService.cerrar(id, actor, request));
    }
}
