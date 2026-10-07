package com.nexora.sport.controller;

import com.nexora.sport.dto.ApartadoPlanRequest;
import com.nexora.sport.dto.ApartadoRequest;
import com.nexora.sport.dto.ApiResponse;
import com.nexora.sport.dto.publico.PublicApartadoPlanResponseDto;
import com.nexora.sport.dto.publico.PublicApartadoResponseDto;
import com.nexora.sport.dto.publico.PublicArticuloApartadoDto;
import com.nexora.sport.dto.publico.PublicCentroDto;
import com.nexora.sport.dto.publico.PublicPlanDto;
import com.nexora.sport.service.ApartadoPlanService;
import com.nexora.sport.service.ApartadoService;
import com.nexora.sport.service.PublicCatalogService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Catalogo publico sin autenticacion: enlace compartible por centro (ej. /catalogo/mi-gimnasio). */
@RestController
@RequestMapping("/api/public/centros/{slug}")
public class PublicController {

    private final PublicCatalogService publicCatalogService;
    private final ApartadoService apartadoService;
    private final ApartadoPlanService apartadoPlanService;

    public PublicController(PublicCatalogService publicCatalogService, ApartadoService apartadoService,
                             ApartadoPlanService apartadoPlanService) {
        this.publicCatalogService = publicCatalogService;
        this.apartadoService = apartadoService;
        this.apartadoPlanService = apartadoPlanService;
    }

    @GetMapping
    public ApiResponse<PublicCentroDto> obtenerCentro(@PathVariable String slug) {
        return ApiResponse.ok(publicCatalogService.obtenerCentro(slug));
    }

    @GetMapping("/apartables")
    public ApiResponse<List<PublicArticuloApartadoDto>> listarApartables(@PathVariable String slug) {
        return ApiResponse.ok(publicCatalogService.listarApartables(slug));
    }

    @GetMapping("/planes")
    public ApiResponse<List<PublicPlanDto>> listarPlanes(@PathVariable String slug) {
        return ApiResponse.ok(publicCatalogService.listarPlanes(slug));
    }

    @PostMapping("/apartados")
    public ApiResponse<PublicApartadoResponseDto> crearApartado(@PathVariable String slug,
                                                                  @Valid @RequestBody ApartadoRequest request) {
        return ApiResponse.ok("Solicitud enviada", apartadoService.crearPublico(slug, request));
    }

    @PostMapping("/apartados-plan")
    public ApiResponse<PublicApartadoPlanResponseDto> crearApartadoPlan(@PathVariable String slug,
                                                                          @Valid @RequestBody ApartadoPlanRequest request) {
        return ApiResponse.ok("Solicitud enviada", apartadoPlanService.crearPublico(slug, request));
    }
}
