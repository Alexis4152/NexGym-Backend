package com.nexora.sport.controller;

import com.nexora.sport.dto.ApiResponse;
import com.nexora.sport.dto.CentroDto;
import com.nexora.sport.dto.CentroRequest;
import com.nexora.sport.service.CentroService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/centros")
public class CentroController {

    private final CentroService centroService;

    public CentroController(CentroService centroService) {
        this.centroService = centroService;
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<List<CentroDto>> listar() {
        return ApiResponse.ok(centroService.listar());
    }

    @GetMapping("/{id}")
    public ApiResponse<CentroDto> obtener(@PathVariable Long id) {
        return ApiResponse.ok(centroService.obtener(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<CentroDto> crear(@Valid @RequestBody CentroRequest request) {
        return ApiResponse.ok("Centro creado", centroService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or @sectionAccess.check('CENTROS')")
    public ApiResponse<CentroDto> actualizar(@PathVariable Long id, @Valid @RequestBody CentroRequest request) {
        return ApiResponse.ok("Centro actualizado", centroService.actualizar(id, request));
    }
}
