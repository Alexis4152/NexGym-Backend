package com.nexora.sport.controller;

import com.nexora.sport.dto.ApiResponse;
import com.nexora.sport.dto.ClaseDisponibleDto;
import com.nexora.sport.dto.ClaseDto;
import com.nexora.sport.dto.ClaseRequest;
import com.nexora.sport.dto.PageResponse;
import com.nexora.sport.model.Usuario;
import com.nexora.sport.service.ClaseService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clases")
@PreAuthorize("@sectionAccess.check('CLASES')")
public class ClaseController {

    private final ClaseService claseService;

    public ClaseController(ClaseService claseService) {
        this.claseService = claseService;
    }

    @GetMapping
    public ApiResponse<PageResponse<ClaseDto>> listar(@AuthenticationPrincipal Usuario actor,
                                                        @RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(claseService.listar(actor, PageRequest.of(page, size)));
    }

    @GetMapping("/activas")
    public ApiResponse<List<ClaseDto>> listarActivas(@AuthenticationPrincipal Usuario actor) {
        return ApiResponse.ok(claseService.listarActivas(actor));
    }

    /** Cupo disponible por clase en TODAS las sucursales del centro (no solo la del
     * actor) -- para mostrador: "en esta sucursal esta lleno, ¿donde mas hay lugar?". */
    @GetMapping("/disponibilidad")
    public ApiResponse<List<ClaseDisponibleDto>> disponibilidad(@AuthenticationPrincipal Usuario actor) {
        return ApiResponse.ok(claseService.disponibilidad(actor));
    }

    @PostMapping
    @PreAuthorize("@sectionAccess.check('CLASES') and @permisoAccess.check('CLASES_CREAR')")
    public ApiResponse<ClaseDto> crear(@AuthenticationPrincipal Usuario actor, @Valid @RequestBody ClaseRequest request) {
        return ApiResponse.ok("Clase creada", claseService.crear(actor, request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@sectionAccess.check('CLASES') and @permisoAccess.check('CLASES_EDITAR')")
    public ApiResponse<ClaseDto> actualizar(@AuthenticationPrincipal Usuario actor, @PathVariable Long id,
                                             @Valid @RequestBody ClaseRequest request) {
        return ApiResponse.ok("Clase actualizada", claseService.actualizar(actor, id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@sectionAccess.check('CLASES') and @permisoAccess.check('CLASES_CANCELAR')")
    public ApiResponse<Void> desactivar(@PathVariable Long id) {
        claseService.desactivar(id);
        return ApiResponse.ok("Clase desactivada", null);
    }
}
