package com.nexora.sport.controller;

import com.nexora.sport.dto.*;
import com.nexora.sport.model.Usuario;
import com.nexora.sport.service.ImagenArticuloService;
import com.nexora.sport.service.InventarioService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/inventario")
@PreAuthorize("@sectionAccess.check('INVENTARIO')")
public class InventarioController {

    private final InventarioService inventarioService;
    private final ImagenArticuloService imagenArticuloService;

    public InventarioController(InventarioService inventarioService, ImagenArticuloService imagenArticuloService) {
        this.inventarioService = inventarioService;
        this.imagenArticuloService = imagenArticuloService;
    }

    @GetMapping("/categorias")
    public ApiResponse<List<CategoriaInventarioDto>> listarCategorias(@AuthenticationPrincipal Usuario actor) {
        return ApiResponse.ok(inventarioService.listarCategorias(actor));
    }

    @PostMapping("/categorias")
    public ApiResponse<CategoriaInventarioDto> crearCategoria(@AuthenticationPrincipal Usuario actor,
                                                                @Valid @RequestBody CategoriaInventarioRequest request) {
        return ApiResponse.ok("Categoria creada", inventarioService.crearCategoria(actor, request));
    }

    @GetMapping("/articulos")
    public ApiResponse<PageResponse<ArticuloInventarioDto>> listar(@AuthenticationPrincipal Usuario actor,
                                                                     @RequestParam(required = false) String q,
                                                                     @RequestParam(defaultValue = "0") int page,
                                                                     @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(inventarioService.listar(actor, q, PageRequest.of(page, size)));
    }

    @GetMapping("/articulos/stock-bajo")
    public ApiResponse<List<ArticuloInventarioDto>> stockBajo(@AuthenticationPrincipal Usuario actor) {
        return ApiResponse.ok(inventarioService.stockBajo(actor));
    }

    @GetMapping("/articulos/por-codigo/{codigoBarras}")
    public ApiResponse<ArticuloInventarioDto> buscarPorCodigoBarras(@AuthenticationPrincipal Usuario actor,
                                                                     @PathVariable String codigoBarras) {
        return ApiResponse.ok(inventarioService.buscarPorCodigoBarras(actor, codigoBarras));
    }

    @PostMapping("/articulos")
    public ApiResponse<ArticuloInventarioDto> crear(@AuthenticationPrincipal Usuario actor,
                                                      @Valid @RequestBody ArticuloInventarioRequest request) {
        return ApiResponse.ok("Articulo creado", inventarioService.crear(actor, request));
    }

    @PutMapping("/articulos/{id}")
    public ApiResponse<ArticuloInventarioDto> actualizar(@PathVariable Long id,
                                                           @Valid @RequestBody ArticuloInventarioRequest request) {
        return ApiResponse.ok("Articulo actualizado", inventarioService.actualizar(id, request));
    }

    @DeleteMapping("/articulos/{id}")
    public ApiResponse<Void> desactivar(@PathVariable Long id) {
        inventarioService.desactivar(id);
        return ApiResponse.ok("Articulo desactivado", null);
    }

    @PostMapping("/articulos/{id}/ajustar-stock")
    public ApiResponse<ArticuloInventarioDto> ajustarStock(@PathVariable Long id, @AuthenticationPrincipal Usuario actor,
                                                             @Valid @RequestBody AjusteStockRequest request) {
        return ApiResponse.ok("Stock actualizado", inventarioService.ajustarStock(id, actor, request));
    }

    @GetMapping("/articulos/{id}/imagenes")
    public ApiResponse<List<ImagenArticuloDto>> listarImagenes(@PathVariable Long id) {
        return ApiResponse.ok(imagenArticuloService.listar(id));
    }

    @PostMapping("/articulos/{id}/imagenes")
    public ApiResponse<ImagenArticuloDto> subirImagen(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return ApiResponse.ok("Foto agregada", imagenArticuloService.subir(inventarioService.buscar(id), file));
    }

    @PutMapping("/articulos/{id}/imagenes/{imagenId}/principal")
    public ApiResponse<Void> marcarPrincipal(@PathVariable Long id, @PathVariable Long imagenId) {
        imagenArticuloService.marcarPrincipal(id, imagenId);
        return ApiResponse.ok("Portada actualizada", null);
    }

    @DeleteMapping("/articulos/{id}/imagenes/{imagenId}")
    public ApiResponse<Void> eliminarImagen(@PathVariable Long id, @PathVariable Long imagenId) {
        imagenArticuloService.eliminar(id, imagenId);
        return ApiResponse.ok("Foto eliminada", null);
    }
}
