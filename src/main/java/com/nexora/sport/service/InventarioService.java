package com.nexora.sport.service;

import com.nexora.sport.dto.*;
import com.nexora.sport.exception.FieldConflictException;
import com.nexora.sport.exception.ResourceNotFoundException;
import com.nexora.sport.model.*;
import com.nexora.sport.repository.*;
import com.nexora.sport.security.TenantScope;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class InventarioService {

    private final ArticuloInventarioRepository articuloRepository;
    private final CategoriaInventarioRepository categoriaRepository;
    private final MovimientoInventarioRepository movimientoRepository;
    private final DisciplinaRepository disciplinaRepository;
    private final CentroRepository centroRepository;
    private final ImagenArticuloRepository imagenArticuloRepository;
    private final TenantScope tenantScope;

    public InventarioService(ArticuloInventarioRepository articuloRepository, CategoriaInventarioRepository categoriaRepository,
                              MovimientoInventarioRepository movimientoRepository, DisciplinaRepository disciplinaRepository,
                              CentroRepository centroRepository, ImagenArticuloRepository imagenArticuloRepository,
                              TenantScope tenantScope) {
        this.articuloRepository = articuloRepository;
        this.categoriaRepository = categoriaRepository;
        this.movimientoRepository = movimientoRepository;
        this.disciplinaRepository = disciplinaRepository;
        this.centroRepository = centroRepository;
        this.imagenArticuloRepository = imagenArticuloRepository;
        this.tenantScope = tenantScope;
    }

    // ---- Categorias ----
    @Transactional(readOnly = true)
    public List<CategoriaInventarioDto> listarCategorias(Usuario actor) {
        return categoriaRepository.findByCentroIdAndActivoTrue(tenantScope.scopeId(actor)).stream()
                .map(c -> new CategoriaInventarioDto(c.getId(), c.getNombre(), c.isActivo())).toList();
    }

    @Transactional
    public CategoriaInventarioDto crearCategoria(Usuario actor, CategoriaInventarioRequest request) {
        CategoriaInventario c = new CategoriaInventario();
        c.setCentro(centroRepository.getReferenceById(tenantScope.scopeId(actor)));
        c.setNombre(request.nombre());
        c = categoriaRepository.save(c);
        return new CategoriaInventarioDto(c.getId(), c.getNombre(), c.isActivo());
    }

    // ---- Articulos ----
    @Transactional(readOnly = true)
    public PageResponse<ArticuloInventarioDto> listar(Usuario actor, String q, Pageable pageable) {
        Long centroId = tenantScope.scopeId(actor);
        var page = (q == null || q.isBlank())
                ? articuloRepository.findByCentroIdAndDeletedAtIsNull(centroId, pageable)
                : articuloRepository.findByCentroIdAndDeletedAtIsNullAndNombreContainingIgnoreCase(centroId, q, pageable);
        return PageResponse.of(page, this::toDto);
    }

    @Transactional(readOnly = true)
    public List<ArticuloInventarioDto> stockBajo(Usuario actor) {
        return articuloRepository.findConStockBajo(tenantScope.scopeId(actor)).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public ArticuloInventarioDto buscarPorCodigoBarras(Usuario actor, String codigoBarras) {
        return articuloRepository.findByCentroIdAndCodigoBarras(tenantScope.scopeId(actor), codigoBarras)
                .map(this::toDto).orElse(null);
    }

    public ArticuloInventario buscar(Long id) {
        return articuloRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Articulo no encontrado"));
    }

    @Transactional
    public ArticuloInventarioDto crear(Usuario actor, ArticuloInventarioRequest request) {
        Long centroId = tenantScope.scopeId(actor);
        validarCodigoBarrasUnico(centroId, request.codigoBarras(), null);
        ArticuloInventario a = new ArticuloInventario();
        a.setCentro(centroRepository.getReferenceById(centroId));
        aplicar(a, request);
        return toDto(articuloRepository.save(a));
    }

    @Transactional
    public ArticuloInventarioDto actualizar(Long id, ArticuloInventarioRequest request) {
        ArticuloInventario a = buscar(id);
        validarCodigoBarrasUnico(a.getCentro().getId(), request.codigoBarras(), id);
        aplicar(a, request);
        return toDto(articuloRepository.save(a));
    }

    private void validarCodigoBarrasUnico(Long centroId, String codigoBarras, Long excludeId) {
        if (codigoBarras == null || codigoBarras.isBlank()) return;
        articuloRepository.findByCentroIdAndCodigoBarras(centroId, codigoBarras)
                .filter(existente -> !existente.getId().equals(excludeId))
                .ifPresent(existente -> {
                    throw new FieldConflictException("codigoBarras", "Ya existe un articulo con ese codigo de barras");
                });
    }

    @Transactional
    public void desactivar(Long id) {
        ArticuloInventario a = buscar(id);
        a.setActivo(false);
        articuloRepository.save(a);
    }

    @Transactional
    public ArticuloInventarioDto ajustarStock(Long id, Usuario actor, AjusteStockRequest request) {
        if (request.delta() < 0 && !tenantScope.isAdminOSuperior(actor)) {
            throw new IllegalStateException("Solo Dueno o Administrador puede quitar piezas del inventario");
        }
        ArticuloInventario a = buscar(id);
        int anterior = a.getStock();
        int nuevo = anterior + request.delta();
        if (nuevo < 0) throw new IllegalArgumentException("El stock no puede quedar negativo");
        a.setStock(nuevo);
        articuloRepository.save(a);

        MovimientoInventario mov = new MovimientoInventario();
        mov.setArticulo(a);
        mov.setTipo(request.delta() >= 0 ? TipoMovimientoInventario.ENTRADA : TipoMovimientoInventario.SALIDA);
        mov.setStockAnterior(anterior);
        mov.setStockNuevo(nuevo);
        mov.setRazon(request.razon());
        mov.setRegistradoPor(actor);
        movimientoRepository.save(mov);

        return toDto(a);
    }

    private void aplicar(ArticuloInventario a, ArticuloInventarioRequest request) {
        a.setCategoria(request.categoriaId() != null ? categoriaRepository.getReferenceById(request.categoriaId()) : null);
        a.setNombre(request.nombre());
        a.setTipo(TipoArticulo.valueOf(request.tipo()));
        a.setCodigoBarras(request.codigoBarras() == null || request.codigoBarras().isBlank() ? null : request.codigoBarras());
        a.setStock(request.stock());
        a.setStockMinimo(request.stockMinimo());
        a.setCosto(request.costo());
        a.setPrecioVenta(request.precioVenta());
        a.setVendible(request.vendible());
        a.setReservable(request.reservable());
        validarDescuentoApartado(a.getCentro(), request.descuentoApartadoPorcentaje());
        a.setDescuentoApartadoPorcentaje(request.descuentoApartadoPorcentaje());
        Set<Disciplina> disciplinas = new HashSet<>();
        if (request.disciplinaIds() != null) {
            request.disciplinaIds().forEach(id -> disciplinas.add(disciplinaRepository.getReferenceById(id)));
        }
        a.setDisciplinas(disciplinas);
    }

    private void validarDescuentoApartado(Centro centro, BigDecimal porcentaje) {
        if (porcentaje == null) return;
        if (porcentaje.compareTo(BigDecimal.ZERO) < 0 || porcentaje.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("El descuento de apartado debe estar entre 0 y 100");
        }
        BigDecimal maximo = centro.getPorcentajeMaximoDescuentoApartado();
        if (maximo != null && porcentaje.compareTo(maximo) > 0) {
            throw new IllegalArgumentException("El descuento de apartado no puede superar " + maximo + "%");
        }
    }

    public ArticuloInventarioDto toDto(ArticuloInventario a) {
        String imagenUrl = imagenArticuloRepository.findFirstByArticuloIdAndEsPrincipalTrue(a.getId())
                .map(ImagenArticulo::getRuta).orElse(a.getImagenUrl());
        return new ArticuloInventarioDto(
                a.getId(), a.getCategoria() != null ? a.getCategoria().getId() : null,
                a.getCategoria() != null ? a.getCategoria().getNombre() : null,
                a.getNombre(), a.getTipo().name(), a.getCodigoBarras(), a.getStock(), a.getStockMinimo(),
                a.getCosto(), a.getPrecioVenta(), a.isVendible(), imagenUrl, a.isReservable(),
                a.getDescuentoApartadoPorcentaje(), a.isActivo(),
                a.getDisciplinas().stream().map(Disciplina::getId).collect(Collectors.toSet()),
                a.getDisciplinas().stream().map(Disciplina::getNombre).collect(Collectors.toSet())
        );
    }
}
