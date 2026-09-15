package com.nexora.sport.controller;

import com.nexora.sport.dto.ApiResponse;
import com.nexora.sport.dto.MembresiaPlanDto;
import com.nexora.sport.dto.MembresiaPlanRequest;
import com.nexora.sport.dto.PageResponse;
import com.nexora.sport.model.Usuario;
import com.nexora.sport.service.MembresiaPlanService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/membresia-planes")
@PreAuthorize("@sectionAccess.check('MEMBRESIAS')")
public class MembresiaPlanController {

    private final MembresiaPlanService membresiaPlanService;

    public MembresiaPlanController(MembresiaPlanService membresiaPlanService) {
        this.membresiaPlanService = membresiaPlanService;
    }

    @GetMapping
    public ApiResponse<PageResponse<MembresiaPlanDto>> listar(@AuthenticationPrincipal Usuario actor,
                                                                @RequestParam(defaultValue = "0") int page,
                                                                @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(membresiaPlanService.listar(actor, PageRequest.of(page, size)));
    }

    @GetMapping("/activos")
    public ApiResponse<List<MembresiaPlanDto>> listarActivos(@AuthenticationPrincipal Usuario actor) {
        return ApiResponse.ok(membresiaPlanService.listarActivos(actor));
    }

    @PostMapping
    public ApiResponse<MembresiaPlanDto> crear(@AuthenticationPrincipal Usuario actor,
                                                @Valid @RequestBody MembresiaPlanRequest request) {
        return ApiResponse.ok("Plan creado", membresiaPlanService.crear(actor, request));
    }

    @PutMapping("/{id}")
    public ApiResponse<MembresiaPlanDto> actualizar(@PathVariable Long id, @Valid @RequestBody MembresiaPlanRequest request) {
        return ApiResponse.ok("Plan actualizado", membresiaPlanService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> desactivar(@PathVariable Long id) {
        membresiaPlanService.desactivar(id);
        return ApiResponse.ok("Plan desactivado", null);
    }
}
