package com.nexora.sport.controller;

import com.nexora.sport.dto.ApiResponse;
import com.nexora.sport.dto.publico.PublicArticuloDto;
import com.nexora.sport.dto.publico.PublicCentroDto;
import com.nexora.sport.service.PublicCatalogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Catalogo publico sin autenticacion: enlace compartible por centro (ej. /catalogo/mi-gimnasio). */
@RestController
@RequestMapping("/api/public/centros/{slug}")
public class PublicController {

    private final PublicCatalogService publicCatalogService;

    public PublicController(PublicCatalogService publicCatalogService) {
        this.publicCatalogService = publicCatalogService;
    }

    @GetMapping
    public ApiResponse<PublicCentroDto> obtenerCentro(@PathVariable String slug) {
        return ApiResponse.ok(publicCatalogService.obtenerCentro(slug));
    }

    @GetMapping("/productos")
    public ApiResponse<List<PublicArticuloDto>> listarProductos(@PathVariable String slug) {
        return ApiResponse.ok(publicCatalogService.listarProductos(slug));
    }
}
