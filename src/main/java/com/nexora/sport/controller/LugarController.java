package com.nexora.sport.controller;

import com.nexora.sport.dto.ApiResponse;
import com.nexora.sport.dto.LugarDto;
import com.nexora.sport.dto.LugarRequest;
import com.nexora.sport.dto.PageResponse;
import com.nexora.sport.model.Usuario;
import com.nexora.sport.service.LugarService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lugares")
@PreAuthorize("@sectionAccess.check('CLASES')")
public class LugarController {

    private final LugarService lugarService;

    public LugarController(LugarService lugarService) {
        this.lugarService = lugarService;
    }

    @GetMapping
    public ApiResponse<PageResponse<LugarDto>> listar(@AuthenticationPrincipal Usuario actor,
                                                        @RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(lugarService.listar(actor, PageRequest.of(page, size)));
    }

    @GetMapping("/activos")
    public ApiResponse<List<LugarDto>> listarActivos(@AuthenticationPrincipal Usuario actor,
                                                       @RequestParam(required = false) Long sucursalId,
                                                       @RequestParam(required = false) Long disciplinaId) {
        return ApiResponse.ok(lugarService.listarActivos(actor, sucursalId, disciplinaId));
    }

    @PostMapping
    public ApiResponse<LugarDto> crear(@AuthenticationPrincipal Usuario actor, @Valid @RequestBody LugarRequest request) {
        return ApiResponse.ok("Lugar creado", lugarService.crear(actor, request));
    }

    @PutMapping("/{id}")
    public ApiResponse<LugarDto> actualizar(@PathVariable Long id, @Valid @RequestBody LugarRequest request) {
        return ApiResponse.ok("Lugar actualizado", lugarService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> desactivar(@PathVariable Long id) {
        lugarService.desactivar(id);
        return ApiResponse.ok("Lugar desactivado", null);
    }
}
