package com.nexora.sport.controller;

import com.nexora.sport.dto.ApartadoPlanCancelRequest;
import com.nexora.sport.dto.ApartadoPlanDto;
import com.nexora.sport.dto.ApiResponse;
import com.nexora.sport.dto.PageResponse;
import com.nexora.sport.model.EstadoApartadoPlan;
import com.nexora.sport.model.Usuario;
import com.nexora.sport.service.ApartadoPlanService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/apartados-plan")
@PreAuthorize("@sectionAccess.check('MEMBRESIAS')")
public class ApartadoPlanController {

    private final ApartadoPlanService apartadoPlanService;

    public ApartadoPlanController(ApartadoPlanService apartadoPlanService) {
        this.apartadoPlanService = apartadoPlanService;
    }

    @GetMapping
    public ApiResponse<PageResponse<ApartadoPlanDto>> listar(@AuthenticationPrincipal Usuario actor,
                                                               @RequestParam(required = false) String estado,
                                                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                                               @RequestParam(required = false) String q,
                                                               @RequestParam(defaultValue = "0") int page,
                                                               @RequestParam(defaultValue = "20") int size) {
        EstadoApartadoPlan filtro = (estado != null && !estado.isBlank()) ? EstadoApartadoPlan.valueOf(estado) : null;
        return ApiResponse.ok(apartadoPlanService.listar(actor, filtro, desde, hasta, q, PageRequest.of(page, size)));
    }

    @GetMapping("/{id}")
    public ApiResponse<ApartadoPlanDto> obtener(@PathVariable Long id) {
        return ApiResponse.ok(apartadoPlanService.obtener(id));
    }

    @PostMapping("/{id}/cancelar")
    public ApiResponse<ApartadoPlanDto> cancelar(@PathVariable Long id, @AuthenticationPrincipal Usuario actor,
                                                  @Valid @RequestBody ApartadoPlanCancelRequest request) {
        return ApiResponse.ok("Apartado cancelado", apartadoPlanService.cancelar(id, actor, request));
    }

    @PostMapping("/{id}/convertir")
    public ApiResponse<ApartadoPlanDto> convertir(@PathVariable Long id, @AuthenticationPrincipal Usuario actor) {
        return ApiResponse.ok("Apartado marcado como convertido", apartadoPlanService.convertir(id, actor));
    }
}
