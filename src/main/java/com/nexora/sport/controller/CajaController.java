package com.nexora.sport.controller;

import com.nexora.sport.dto.*;
import com.nexora.sport.model.TipoMovimiento;
import com.nexora.sport.model.Usuario;
import com.nexora.sport.service.CajaService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/caja")
@PreAuthorize("@sectionAccess.check('CAJA')")
public class CajaController {

    private final CajaService cajaService;

    public CajaController(CajaService cajaService) {
        this.cajaService = cajaService;
    }

    @GetMapping("/categorias")
    public ApiResponse<List<CategoriaMovimientoDto>> listarCategorias(@AuthenticationPrincipal Usuario actor,
                                                                        @RequestParam(required = false) String tipo) {
        return ApiResponse.ok(cajaService.listarCategorias(actor, tipo != null ? TipoMovimiento.valueOf(tipo) : null));
    }

    @PostMapping("/categorias")
    public ApiResponse<CategoriaMovimientoDto> crearCategoria(@AuthenticationPrincipal Usuario actor,
                                                                @Valid @RequestBody CategoriaMovimientoRequest request) {
        return ApiResponse.ok("Categoria creada", cajaService.crearCategoria(actor, request));
    }

    @GetMapping("/movimientos")
    public ApiResponse<PageResponse<MovimientoFinancieroDto>> listarMovimientos(
            @AuthenticationPrincipal Usuario actor,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(cajaService.listarMovimientos(actor, desde, hasta, PageRequest.of(page, size)));
    }

    @PostMapping("/ingresos")
    @PreAuthorize("@sectionAccess.check('CAJA') and @permisoAccess.check('CAJA_MOVIMIENTO')")
    public ApiResponse<MovimientoFinancieroDto> registrarIngreso(@AuthenticationPrincipal Usuario actor,
                                                                   @Valid @RequestBody MovimientoFinancieroRequest request) {
        return ApiResponse.ok("Ingreso registrado", cajaService.registrarMovimiento(actor, TipoMovimiento.INGRESO, request));
    }

    @PostMapping("/egresos")
    @PreAuthorize("@sectionAccess.check('CAJA') and @permisoAccess.check('CAJA_MOVIMIENTO')")
    public ApiResponse<MovimientoFinancieroDto> registrarEgreso(@AuthenticationPrincipal Usuario actor,
                                                                  @Valid @RequestBody MovimientoFinancieroRequest request) {
        return ApiResponse.ok("Egreso registrado", cajaService.registrarMovimiento(actor, TipoMovimiento.EGRESO, request));
    }
}
