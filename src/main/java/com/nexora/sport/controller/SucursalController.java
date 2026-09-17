package com.nexora.sport.controller;

import com.nexora.sport.dto.ApiResponse;
import com.nexora.sport.dto.PageResponse;
import com.nexora.sport.dto.SucursalDto;
import com.nexora.sport.dto.SucursalRequest;
import com.nexora.sport.model.Usuario;
import com.nexora.sport.service.SucursalService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sucursales")
@PreAuthorize("@sectionAccess.check('CLASES')")
public class SucursalController {

    private final SucursalService sucursalService;

    public SucursalController(SucursalService sucursalService) {
        this.sucursalService = sucursalService;
    }

    @GetMapping
    public ApiResponse<PageResponse<SucursalDto>> listar(@AuthenticationPrincipal Usuario actor,
                                                           @RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(sucursalService.listar(actor, PageRequest.of(page, size)));
    }

    @GetMapping("/activas")
    public ApiResponse<List<SucursalDto>> listarActivas(@AuthenticationPrincipal Usuario actor) {
        return ApiResponse.ok(sucursalService.listarActivas(actor));
    }

    @PostMapping
    public ApiResponse<SucursalDto> crear(@AuthenticationPrincipal Usuario actor, @Valid @RequestBody SucursalRequest request) {
        return ApiResponse.ok("Sucursal creada", sucursalService.crear(actor, request));
    }

    @PutMapping("/{id}")
    public ApiResponse<SucursalDto> actualizar(@PathVariable Long id, @Valid @RequestBody SucursalRequest request) {
        return ApiResponse.ok("Sucursal actualizada", sucursalService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> desactivar(@PathVariable Long id) {
        sucursalService.desactivar(id);
        return ApiResponse.ok("Sucursal desactivada", null);
    }
}
