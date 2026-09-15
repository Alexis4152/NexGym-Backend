package com.nexora.sport.controller;

import com.nexora.sport.dto.ApiResponse;
import com.nexora.sport.dto.AsistenciaDto;
import com.nexora.sport.dto.AsistenciaRequest;
import com.nexora.sport.dto.PageResponse;
import com.nexora.sport.model.Usuario;
import com.nexora.sport.service.AsistenciaService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/asistencias")
@PreAuthorize("@sectionAccess.check('ASISTENCIA')")
public class AsistenciaController {

    private final AsistenciaService asistenciaService;

    public AsistenciaController(AsistenciaService asistenciaService) {
        this.asistenciaService = asistenciaService;
    }

    @GetMapping
    public ApiResponse<PageResponse<AsistenciaDto>> listar(@AuthenticationPrincipal Usuario actor,
                                                             @RequestParam(defaultValue = "0") int page,
                                                             @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(asistenciaService.listar(actor, PageRequest.of(page, size)));
    }

    @PostMapping
    public ApiResponse<AsistenciaDto> registrar(@AuthenticationPrincipal Usuario actor, @Valid @RequestBody AsistenciaRequest request) {
        return ApiResponse.ok("Asistencia registrada", asistenciaService.registrar(actor, request));
    }
}
