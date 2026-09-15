package com.nexora.sport.controller;

import com.nexora.sport.dto.ApiResponse;
import com.nexora.sport.dto.PageResponse;
import com.nexora.sport.dto.ProveedorDto;
import com.nexora.sport.dto.ProveedorRequest;
import com.nexora.sport.model.Usuario;
import com.nexora.sport.service.ProveedorService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/proveedores")
@PreAuthorize("@sectionAccess.check('COMPRAS')")
public class ProveedorController {

    private final ProveedorService proveedorService;

    public ProveedorController(ProveedorService proveedorService) {
        this.proveedorService = proveedorService;
    }

    @GetMapping
    public ApiResponse<PageResponse<ProveedorDto>> listar(@AuthenticationPrincipal Usuario actor,
                                                            @RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(proveedorService.listar(actor, PageRequest.of(page, size)));
    }

    @PostMapping
    public ApiResponse<ProveedorDto> crear(@AuthenticationPrincipal Usuario actor, @Valid @RequestBody ProveedorRequest request) {
        return ApiResponse.ok("Proveedor creado", proveedorService.crear(actor, request));
    }

    @PutMapping("/{id}")
    public ApiResponse<ProveedorDto> actualizar(@PathVariable Long id, @Valid @RequestBody ProveedorRequest request) {
        return ApiResponse.ok("Proveedor actualizado", proveedorService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> desactivar(@PathVariable Long id) {
        proveedorService.desactivar(id);
        return ApiResponse.ok("Proveedor desactivado", null);
    }
}
