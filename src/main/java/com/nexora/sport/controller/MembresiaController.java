package com.nexora.sport.controller;

import com.nexora.sport.dto.ApiResponse;
import com.nexora.sport.dto.MembresiaDto;
import com.nexora.sport.dto.MembresiaRequest;
import com.nexora.sport.dto.PageResponse;
import com.nexora.sport.model.Usuario;
import com.nexora.sport.service.MembresiaService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/membresias")
@PreAuthorize("@sectionAccess.check('MEMBRESIAS')")
public class MembresiaController {

    private final MembresiaService membresiaService;

    public MembresiaController(MembresiaService membresiaService) {
        this.membresiaService = membresiaService;
    }

    @GetMapping
    public ApiResponse<PageResponse<MembresiaDto>> listar(@AuthenticationPrincipal Usuario actor,
                                                            @RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(membresiaService.listar(actor, PageRequest.of(page, size)));
    }

    @GetMapping("/alumno/{alumnoId}")
    public ApiResponse<PageResponse<MembresiaDto>> listarPorAlumno(@PathVariable Long alumnoId,
                                                                     @RequestParam(defaultValue = "0") int page,
                                                                     @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(membresiaService.listarPorAlumno(alumnoId, PageRequest.of(page, size)));
    }

    @GetMapping("/proximas-a-vencer")
    public ApiResponse<List<MembresiaDto>> proximasAVencer(@AuthenticationPrincipal Usuario actor,
                                                             @RequestParam(defaultValue = "7") int dias) {
        return ApiResponse.ok(membresiaService.proximasAVencer(actor, dias));
    }

    @PostMapping
    public ApiResponse<MembresiaDto> crear(@AuthenticationPrincipal Usuario actor, @Valid @RequestBody MembresiaRequest request) {
        return ApiResponse.ok("Membresia registrada", membresiaService.crear(actor, request));
    }

    @PostMapping("/{id}/cancelar")
    public ApiResponse<MembresiaDto> cancelar(@PathVariable Long id) {
        return ApiResponse.ok("Membresia cancelada", membresiaService.cancelar(id));
    }
}
