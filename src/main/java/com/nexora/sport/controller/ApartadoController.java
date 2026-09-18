package com.nexora.sport.controller;

import com.nexora.sport.dto.*;
import com.nexora.sport.model.EstadoApartado;
import com.nexora.sport.model.Usuario;
import com.nexora.sport.service.ApartadoService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/apartados")
@PreAuthorize("@sectionAccess.check('TIENDA')")
public class ApartadoController {

    private final ApartadoService apartadoService;

    public ApartadoController(ApartadoService apartadoService) {
        this.apartadoService = apartadoService;
    }

    @GetMapping
    public ApiResponse<PageResponse<ApartadoDto>> listar(@AuthenticationPrincipal Usuario actor,
                                                           @RequestParam(required = false) String estado,
                                                           @RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "20") int size) {
        EstadoApartado filtro = estado != null ? EstadoApartado.valueOf(estado) : null;
        return ApiResponse.ok(apartadoService.listar(actor, filtro, PageRequest.of(page, size)));
    }

    @GetMapping("/{id}")
    public ApiResponse<ApartadoDto> obtener(@PathVariable Long id) {
        return ApiResponse.ok(apartadoService.obtener(id));
    }

    @PostMapping("/{id}/confirmar")
    public ApiResponse<ApartadoDto> confirmar(@PathVariable Long id, @AuthenticationPrincipal Usuario actor,
                                               @Valid @RequestBody ApartadoConfirmRequest request) {
        return ApiResponse.ok("Apartado confirmado", apartadoService.confirmar(id, actor, request));
    }

    @PostMapping("/{id}/completar")
    public ApiResponse<ApartadoDto> completar(@PathVariable Long id, @AuthenticationPrincipal Usuario actor,
                                               @Valid @RequestBody ApartadoCompleteRequest request) {
        return ApiResponse.ok("Apartado completado", apartadoService.completar(id, actor, request));
    }

    @PostMapping("/{id}/cancelar")
    public ApiResponse<ApartadoDto> cancelar(@PathVariable Long id, @AuthenticationPrincipal Usuario actor,
                                              @Valid @RequestBody ApartadoCancelRequest request) {
        return ApiResponse.ok("Apartado cancelado", apartadoService.cancelar(id, actor, request));
    }

    @DeleteMapping("/{id}/items/{itemId}")
    public ApiResponse<ApartadoDto> eliminarItem(@PathVariable Long id, @PathVariable Long itemId) {
        return ApiResponse.ok("Articulo quitado del apartado", apartadoService.eliminarItem(id, itemId));
    }
}
