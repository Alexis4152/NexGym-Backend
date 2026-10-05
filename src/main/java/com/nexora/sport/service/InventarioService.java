package com.nexora.sport.service;

import com.nexora.sport.dto.*;
import com.nexora.sport.exception.FieldConflictException;
import com.nexora.sport.exception.ResourceNotFoundException;
import com.nexora.sport.model.*;
import com.nexora.sport.repository.*;
import com.nexora.sport.security.PermisoEvaluator;
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
    private final SucursalRepository sucursalRepository;
    private final ImagenArticuloRepository imagenArticuloRepository;
    private final TenantScope tenantScope;
    private final NotificacionService notificacionService;

    public InventarioService(ArticuloInventarioRepository articuloRepository, CategoriaInventarioRepository categoriaRepository,
                              MovimientoInventarioRepository movimientoRepository, DisciplinaRepository disciplinaRepository,
                              CentroRepository centroRepository, SucursalRepository sucursalRepository,
                              ImagenArticuloRepository imagenArticuloRepository,
                              TenantScope tenantScope, NotificacionService notificacionService) {
        this.articuloRepository = articuloRepository;
        this.categoriaRepository = categoriaRepository;
        this.movimientoRepository = movimientoRepository;
        this.disciplinaRepository = disciplinaRepository;
        this.centroRepository = centroRepository;
        this.sucursalRepository = sucursalRepository;
        this.imagenArticuloRepository = imagenArticuloRepository;
        this.tenantScope = tenantScope;
        this.notificacionService = notificacionService;
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
    public PageResponse<ArticuloInventarioDto> listar(Usuario actor, String q, Long categoriaId, boolean soloStockBajo,
                                                        Long sucursalIdFiltro, Pageable pageable) {
        Long centroId = tenantScope.scopeId(actor);
        Long sucursalId = sucursalEfectiva(actor, sucursalIdFiltro);
        String texto = (q == null || q.isBlank()) ? null : q;
        return PageResponse.of(articuloRepository.buscar(centroId, sucursalId, texto, categoriaId, soloStockBajo, pageable), this::toDto);
    }

    @Transactional(readOnly = true)
    public List<ArticuloInventarioDto> stockBajo(Usuario actor, Long sucursalIdFiltro) {
        Long sucursalId = sucursalEfectiva(actor, sucursalIdFiltro);
        return articuloRepository.findConStockBajo(tenantScope.scopeId(actor), sucursalId).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public ArticuloInventarioDto buscarPorCodigoBarras(Usuario actor, String codigoBarras) {
        return articuloRepository.findByCentroIdAndCodigoBarras(tenantScope.scopeId(actor), codigoBarras)
                .filter(a -> tenantScope.sucursalPermite(actor, a.getSucursal().getId()))
                .map(this::toDto).orElse(null);
    }

    /** Busqueda de solo lectura en el inventario de las DEMAS sucursales del centro (seccion
     * 31 del encargo): para cuando un articulo no existe o no tiene stock en la sucursal
     * del que busca -- nunca se puede vender desde aqui, solo informa donde si hay. */
    @Transactional(readOnly = true)
    public List<ArticuloOtraSucursalDto> buscarEnOtrasSucursales(Usuario actor, String q) {
        Long centroId = tenantScope.scopeId(actor);
        Long miSucursal = tenantScope.sucursalActivaId(actor);
        if (miSucursal == null) return List.of(); // Dueno/SUPER_ADMIN ya ven todas las sucursales de por si
        return articuloRepository.buscarEnOtrasSucursales(centroId, miSucursal, q).stream()
                .filter(a -> a.getStock() > 0)
                .map(a -> new ArticuloOtraSucursalDto(a.getId(), a.getNombre(), a.getSucursal().getId(), a.getSucursal().getNombre(), a.getStock()))
                .toList();
    }

    public ArticuloInventario buscar(Long id) {
        return articuloRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Articulo no encontrado"));
    }

    /** Nunca devuelve un articulo de otra sucursal (aislamiento de "cada sucursal su propio
     * inventario"): un Dueno/SUPER_ADMIN sin restriccion si puede, un Encargado/Recepcion solo
     * si el articulo pertenece a una de sus sucursales autorizadas. */
    public ArticuloInventario buscarEnAlcance(Usuario actor, Long id) {
        ArticuloInventario a = buscar(id);
        if (!tenantScope.sucursalPermite(actor, a.getSucursal().getId())) {
            throw new ResourceNotFoundException("Articulo no encontrado");
        }
        return a;
    }

    /** La sucursal sobre la que opera esta peticion: la activa del actor si esta restringido
     * a una, o -- solo para un actor sin restriccion (Dueno/SUPER_ADMIN) -- el filtro opcional
     * que haya elegido en la pantalla ("ver todas" vs "ver esta sucursal"). Nunca se confia en
     * el filtro para un actor ya restringido a su(s) propia(s) sucursal(es). */
    private Long sucursalEfectiva(Usuario actor, Long sucursalIdFiltro) {
        Long activa = tenantScope.sucursalActivaId(actor);
        return activa != null ? activa : sucursalIdFiltro;
    }

    @Transactional
    public ArticuloInventarioDto crear(Usuario actor, ArticuloInventarioRequest request) {
        Long centroId = tenantScope.scopeId(actor);
        Long sucursalId = sucursalEfectiva(actor, request.sucursalId());
        if (sucursalId == null) {
            throw new IllegalStateException("Selecciona una sucursal para crear el articulo");
        }
        Sucursal sucursal = sucursalRepository.findById(sucursalId)
                .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada"));
        if (!sucursal.getCentro().getId().equals(centroId)) {
            throw new ResourceNotFoundException("Sucursal no encontrada");
        }
        validarCodigoBarrasUnico(centroId, request.codigoBarras(), null);
        ArticuloInventario a = new ArticuloInventario();
        a.setCentro(centroRepository.getReferenceById(centroId));
        a.setSucursal(sucursal);
        aplicar(a, request);
        a = articuloRepository.save(a);
        notificacionService.notificarAdminProductoNuevo(a);
        return toDto(a);
    }

    @Transactional
    public ArticuloInventarioDto actualizar(Usuario actor, Long id, ArticuloInventarioRequest request) {
        ArticuloInventario a = buscarEnAlcance(actor, id);
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
    public void desactivar(Usuario actor, Long id) {
        ArticuloInventario a = buscarEnAlcance(actor, id);
        a.setActivo(false);
        articuloRepository.save(a);
    }

    @Transactional
    public ArticuloInventarioDto ajustarStock(Long id, Usuario actor, AjusteStockRequest request) {
        if (request.delta() < 0 && !PermisoEvaluator.tiene(actor, Permiso.INVENTARIO_AJUSTE_NEGATIVO)) {
            throw new IllegalStateException("No tienes permiso para quitar piezas del inventario");
        }
        if (request.delta() > 0 && !PermisoEvaluator.tiene(actor, Permiso.INVENTARIO_ENTRADA)) {
            throw new IllegalStateException("No tienes permiso para agregar piezas al inventario");
        }
        ArticuloInventario a = buscarEnAlcance(actor, id);
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

        verificarUmbralesStock(a, anterior);
        return toDto(a);
    }

    /** Avisa cuando el stock CRUZA un umbral (no en cada movimiento mientras se queda del
     * mismo lado): se agoto, se recupero, o cayo a stock bajo. La usa tanto
     * ajustarStock() como VentaService al descontar por una venta. */
    public void verificarUmbralesStock(ArticuloInventario a, int stockAnterior) {
        int nuevo = a.getStock();
        if (stockAnterior > 0 && nuevo == 0) {
            notificacionService.notificarAdminStockAgotado(a);
        } else if (stockAnterior == 0 && nuevo > 0) {
            notificacionService.notificarAdminStockRecuperado(a);
        } else if (nuevo > 0 && nuevo <= a.getStockMinimo() && stockAnterior > a.getStockMinimo()) {
            notificacionService.notificarAdminStockBajo(a);
        }
    }

    private void aplicar(ArticuloInventario a, ArticuloInventarioRequest request) {
        Set<CategoriaInventario> categorias = new HashSet<>();
        if (request.categoriaIds() != null) {
            request.categoriaIds().forEach(id -> categorias.add(categoriaRepository.getReferenceById(id)));
        }
        a.setCategorias(categorias);
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
                a.getId(),
                a.getSucursal() != null ? a.getSucursal().getId() : null,
                a.getSucursal() != null ? a.getSucursal().getNombre() : null,
                a.getCategorias().stream().map(CategoriaInventario::getId).collect(Collectors.toSet()),
                a.getCategorias().stream().map(CategoriaInventario::getNombre).collect(Collectors.toSet()),
                a.getNombre(), a.getTipo().name(), a.getCodigoBarras(), a.getStock(), a.getStockMinimo(),
                a.getCosto(), a.getPrecioVenta(), a.isVendible(), imagenUrl, a.isReservable(),
                a.getDescuentoApartadoPorcentaje(), a.isActivo(),
                a.getDisciplinas().stream().map(Disciplina::getId).collect(Collectors.toSet()),
                a.getDisciplinas().stream().map(Disciplina::getNombre).collect(Collectors.toSet())
        );
    }
}
