package com.nexora.sport.controller;

import com.nexora.sport.dto.ApiResponse;
import com.nexora.sport.dto.CompraDto;
import com.nexora.sport.dto.CompraRequest;
import com.nexora.sport.dto.PageResponse;
import com.nexora.sport.model.Usuario;
import com.nexora.sport.service.CompraService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/compras")
@PreAuthorize("@sectionAccess.check('COMPRAS')")
public class CompraController {

    private final CompraService compraService;

    public CompraController(CompraService compraService) {
        this.compraService = compraService;
    }

    @GetMapping
    public ApiResponse<PageResponse<CompraDto>> listar(@AuthenticationPrincipal Usuario actor,
                                                         @RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(compraService.listar(actor, PageRequest.of(page, size)));
    }

    @PostMapping
    public ApiResponse<CompraDto> crear(@AuthenticationPrincipal Usuario actor, @Valid @RequestBody CompraRequest request) {
        return ApiResponse.ok("Compra registrada", compraService.crear(actor, request));
    }

    @PostMapping("/{id}/marcar-realizada")
    public ApiResponse<CompraDto> marcarRealizada(@PathVariable Long id, @AuthenticationPrincipal Usuario actor) {
        return ApiResponse.ok("Compra marcada como realizada; se actualizo el inventario y la caja",
                compraService.marcarRealizada(id, actor));
    }
}
