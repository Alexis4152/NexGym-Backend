package com.nexora.sport.controller;

import com.nexora.sport.dto.AlumnoDto;
import com.nexora.sport.dto.ApartadoPlanCancelRequest;
import com.nexora.sport.dto.ApartadoPlanDto;
import com.nexora.sport.dto.ApiResponse;
import com.nexora.sport.dto.ConvertirApartadoPlanRequest;
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
import java.util.List;

/** Ver los apartados de plan: quien tenga Tienda (vive en la pantalla de Apartados, ahi
 * tambien entra Caja/Ventas) o Membresias. Procesarlos (convertir/cancelar) es exclusivo de
 * Membresias, porque eso da de alta o anula una membresia/anticipo. */
@RestController
@RequestMapping("/api/apartados-plan")
@PreAuthorize("@sectionAccess.check('TIENDA') or @sectionAccess.check('MEMBRESIAS')")
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
    public ApiResponse<ApartadoPlanDto> obtener(@PathVariable Long id, @AuthenticationPrincipal Usuario actor) {
        return ApiResponse.ok(apartadoPlanService.obtener(id, actor));
    }

    @PostMapping("/{id}/cancelar")
    @PreAuthorize("@sectionAccess.check('MEMBRESIAS')")
    public ApiResponse<ApartadoPlanDto> cancelar(@PathVariable Long id, @AuthenticationPrincipal Usuario actor,
                                                  @Valid @RequestBody ApartadoPlanCancelRequest request) {
        return ApiResponse.ok("Apartado cancelado", apartadoPlanService.cancelar(id, actor, request));
    }

    /** Alumnos que podrian ser el cliente del apartado (mismo correo/telefono/nombre). */
    @GetMapping("/{id}/alumnos-coincidentes")
    @PreAuthorize("@sectionAccess.check('MEMBRESIAS')")
    public ApiResponse<List<AlumnoDto>> alumnosCoincidentes(@PathVariable Long id, @AuthenticationPrincipal Usuario actor) {
        return ApiResponse.ok(apartadoPlanService.alumnosCoincidentes(id, actor));
    }

    /** Da de alta la membresia del apartado ligada a un alumno existente o nuevo. */
    @PostMapping("/{id}/convertir")
    @PreAuthorize("@sectionAccess.check('MEMBRESIAS') and @permisoAccess.check('MEMBRESIAS_CREAR')")
    public ApiResponse<ApartadoPlanDto> convertir(@PathVariable Long id, @AuthenticationPrincipal Usuario actor,
                                                   @Valid @RequestBody ConvertirApartadoPlanRequest request) {
        return ApiResponse.ok("Membresia dada de alta", apartadoPlanService.convertir(id, actor, request));
    }
}
