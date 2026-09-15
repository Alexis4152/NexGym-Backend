package com.nexora.sport.controller;

import com.nexora.sport.dto.ApiResponse;
import com.nexora.sport.dto.PageResponse;
import com.nexora.sport.dto.ReservaDto;
import com.nexora.sport.dto.ReservaRequest;
import com.nexora.sport.model.EstadoReserva;
import com.nexora.sport.model.Usuario;
import com.nexora.sport.service.ReservaService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reservas")
@PreAuthorize("@sectionAccess.check('CLASES')")
public class ReservaController {

    private final ReservaService reservaService;

    public ReservaController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    @GetMapping
    public ApiResponse<PageResponse<ReservaDto>> listar(@AuthenticationPrincipal Usuario actor,
                                                          @RequestParam(defaultValue = "0") int page,
                                                          @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(reservaService.listar(actor, PageRequest.of(page, size)));
    }

    @GetMapping("/alumno/{alumnoId}")
    public ApiResponse<PageResponse<ReservaDto>> listarPorAlumno(@PathVariable Long alumnoId,
                                                                   @RequestParam(defaultValue = "0") int page,
                                                                   @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(reservaService.listarPorAlumno(alumnoId, PageRequest.of(page, size)));
    }

    @PostMapping
    public ApiResponse<ReservaDto> crear(@AuthenticationPrincipal Usuario actor, @Valid @RequestBody ReservaRequest request) {
        return ApiResponse.ok("Reserva creada", reservaService.crear(actor, request));
    }

    @PutMapping("/{id}/estado")
    public ApiResponse<ReservaDto> cambiarEstado(@PathVariable Long id, @RequestParam String estado) {
        return ApiResponse.ok("Reserva actualizada", reservaService.cambiarEstado(id, EstadoReserva.valueOf(estado)));
    }
}
