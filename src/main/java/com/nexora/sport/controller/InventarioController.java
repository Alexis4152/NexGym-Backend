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
    @PreAuthorize("@sectionAccess.check('INVENTARIO') and @permisoAccess.check('INVENTARIO_CREAR')")
    public ApiResponse<CategoriaInventarioDto> crearCategoria(@AuthenticationPrincipal Usuario actor,
                                                                @Valid @RequestBody CategoriaInventarioRequest request) {
        return ApiResponse.ok("Categoria creada", inventarioService.crearCategoria(actor, request));
    }

    @GetMapping("/articulos")
    public ApiResponse<PageResponse<ArticuloInventarioDto>> listar(@AuthenticationPrincipal Usuario actor,
                                                                     @RequestParam(required = false) String q,
                                                                     @RequestParam(required = false) Long categoriaId,
                                                                     @RequestParam(defaultValue = "false") boolean soloStockBajo,
                                                                     @RequestParam(required = false) Long sucursalId,
                                                                     @RequestParam(defaultValue = "0") int page,
                                                                     @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(inventarioService.listar(actor, q, categoriaId, soloStockBajo, sucursalId, PageRequest.of(page, size)));
    }

    @GetMapping("/articulos/stock-bajo")
    public ApiResponse<List<ArticuloInventarioDto>> stockBajo(@AuthenticationPrincipal Usuario actor,
                                                                @RequestParam(required = false) Long sucursalId) {
        return ApiResponse.ok(inventarioService.stockBajo(actor, sucursalId));
    }

    @GetMapping("/articulos/por-codigo/{codigoBarras}")
    public ApiResponse<ArticuloInventarioDto> buscarPorCodigoBarras(@AuthenticationPrincipal Usuario actor,
                                                                     @PathVariable String codigoBarras) {
        return ApiResponse.ok(inventarioService.buscarPorCodigoBarras(actor, codigoBarras));
    }

    @GetMapping("/articulos/otras-sucursales")
    public ApiResponse<List<ArticuloOtraSucursalDto>> buscarEnOtrasSucursales(@AuthenticationPrincipal Usuario actor,
                                                                                @RequestParam String q) {
        return ApiResponse.ok(inventarioService.buscarEnOtrasSucursales(actor, q));
    }

    @PostMapping("/articulos")
    @PreAuthorize("@sectionAccess.check('INVENTARIO') and @permisoAccess.check('INVENTARIO_CREAR')")
    public ApiResponse<ArticuloInventarioDto> crear(@AuthenticationPrincipal Usuario actor,
                                                      @Valid @RequestBody ArticuloInventarioRequest request) {
        return ApiResponse.ok("Articulo creado", inventarioService.crear(actor, request));
    }

    @PutMapping("/articulos/{id}")
    @PreAuthorize("@sectionAccess.check('INVENTARIO') and @permisoAccess.check('INVENTARIO_EDITAR')")
    public ApiResponse<ArticuloInventarioDto> actualizar(@AuthenticationPrincipal Usuario actor, @PathVariable Long id,
                                                           @Valid @RequestBody ArticuloInventarioRequest request) {
        return ApiResponse.ok("Articulo actualizado", inventarioService.actualizar(actor, id, request));
    }

    @DeleteMapping("/articulos/{id}")
    @PreAuthorize("@sectionAccess.check('INVENTARIO') and @permisoAccess.check('INVENTARIO_EDITAR')")
    public ApiResponse<Void> desactivar(@AuthenticationPrincipal Usuario actor, @PathVariable Long id) {
        inventarioService.desactivar(actor, id);
        return ApiResponse.ok("Articulo desactivado", null);
    }

    @PostMapping("/articulos/{id}/ajustar-stock")
    public ApiResponse<ArticuloInventarioDto> ajustarStock(@PathVariable Long id, @AuthenticationPrincipal Usuario actor,
                                                             @Valid @RequestBody AjusteStockRequest request) {
        return ApiResponse.ok("Stock actualizado", inventarioService.ajustarStock(id, actor, request));
    }

    @GetMapping("/articulos/{id}/imagenes")
    public ApiResponse<List<ImagenArticuloDto>> listarImagenes(@AuthenticationPrincipal Usuario actor, @PathVariable Long id) {
        inventarioService.buscarEnAlcance(actor, id);
        return ApiResponse.ok(imagenArticuloService.listar(id));
    }

    @PostMapping("/articulos/{id}/imagenes")
    @PreAuthorize("@sectionAccess.check('INVENTARIO') and @permisoAccess.check('INVENTARIO_EDITAR')")
    public ApiResponse<ImagenArticuloDto> subirImagen(@AuthenticationPrincipal Usuario actor, @PathVariable Long id,
                                                        @RequestParam("file") MultipartFile file) {
        return ApiResponse.ok("Foto agregada", imagenArticuloService.subir(inventarioService.buscarEnAlcance(actor, id), file));
    }

    @PutMapping("/articulos/{id}/imagenes/{imagenId}/principal")
    @PreAuthorize("@sectionAccess.check('INVENTARIO') and @permisoAccess.check('INVENTARIO_EDITAR')")
    public ApiResponse<Void> marcarPrincipal(@AuthenticationPrincipal Usuario actor, @PathVariable Long id, @PathVariable Long imagenId) {
        inventarioService.buscarEnAlcance(actor, id);
        imagenArticuloService.marcarPrincipal(id, imagenId);
        return ApiResponse.ok("Portada actualizada", null);
    }

    @DeleteMapping("/articulos/{id}/imagenes/{imagenId}")
    @PreAuthorize("@sectionAccess.check('INVENTARIO') and @permisoAccess.check('INVENTARIO_EDITAR')")
    public ApiResponse<Void> eliminarImagen(@AuthenticationPrincipal Usuario actor, @PathVariable Long id, @PathVariable Long imagenId) {
        inventarioService.buscarEnAlcance(actor, id);
        imagenArticuloService.eliminar(id, imagenId);
        return ApiResponse.ok("Foto eliminada", null);
    }
}
