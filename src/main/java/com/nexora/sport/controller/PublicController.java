package com.nexora.sport.controller;

import com.nexora.sport.dto.ApartadoPlanRequest;
import com.nexora.sport.dto.ApartadoRequest;
import com.nexora.sport.dto.ApiResponse;
import com.nexora.sport.dto.publico.ApartadoConsultaRequest;
import com.nexora.sport.dto.publico.ApartadoTelefonoRequest;
import com.nexora.sport.dto.publico.PublicApartadoConsultaDto;
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

    /** "Consultar mi apartado": estado de un apartado por folio + telefono (POST para no dejar
     * el telefono en la URL/logs). */
    @PostMapping("/apartados/consulta")
    public ApiResponse<PublicApartadoConsultaDto> consultarApartado(@PathVariable String slug,
                                                                     @Valid @RequestBody ApartadoConsultaRequest request) {
        return ApiResponse.ok(apartadoService.consultarPublico(slug, request.folio(), request.telefono()));
    }

    /** "¿No tienes tu folio?": apartados recientes del centro con ese telefono (hasta 10). */
    @PostMapping("/apartados/consulta-telefono")
    public ApiResponse<List<PublicApartadoConsultaDto>> buscarApartadosPorTelefono(@PathVariable String slug,
                                                                                    @Valid @RequestBody ApartadoTelefonoRequest request) {
        return ApiResponse.ok(apartadoService.buscarPorTelefonoPublico(slug, request.telefono()));
    }

    /** El cliente cancela su propio apartado (PENDIENTE o ACTIVO); folio+telefono se revalidan. */
    @PostMapping("/apartados/cancelar")
    public ApiResponse<PublicApartadoConsultaDto> cancelarApartado(@PathVariable String slug,
                                                                    @Valid @RequestBody ApartadoConsultaRequest request) {
        return ApiResponse.ok("Tu apartado fue cancelado",
                apartadoService.cancelarPublico(slug, request.folio(), request.telefono(), request.motivo()));
    }

    @PostMapping("/apartados-plan")
    public ApiResponse<PublicApartadoPlanResponseDto> crearApartadoPlan(@PathVariable String slug,
                                                                          @Valid @RequestBody ApartadoPlanRequest request) {
        return ApiResponse.ok("Solicitud enviada", apartadoPlanService.crearPublico(slug, request));
    }
}
