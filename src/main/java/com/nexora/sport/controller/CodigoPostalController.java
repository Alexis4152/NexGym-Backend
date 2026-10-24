package com.nexora.sport.controller;

import com.nexora.sport.dto.ApiResponse;
import com.nexora.sport.dto.CodigoPostalDto;
import com.nexora.sport.exception.ResourceNotFoundException;
import com.nexora.sport.service.CodigoPostalService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Autollenado del formulario de domicilio (estado, municipio y colonias a partir del C.P.). */
@RestController
@RequestMapping("/api/codigos-postales")
public class CodigoPostalController {

    private final CodigoPostalService codigoPostalService;

    public CodigoPostalController(CodigoPostalService codigoPostalService) {
        this.codigoPostalService = codigoPostalService;
    }

    @GetMapping("/{cp}")
    public ApiResponse<CodigoPostalDto> buscar(@PathVariable String cp) {
        return ApiResponse.ok(codigoPostalService.buscar(cp)
                .orElseThrow(() -> new ResourceNotFoundException("No encontramos el codigo postal " + cp)));
    }
}
