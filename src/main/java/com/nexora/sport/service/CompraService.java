package com.nexora.sport.service;

import com.nexora.sport.dto.CompraDto;
import com.nexora.sport.dto.CompraRequest;
import com.nexora.sport.dto.PageResponse;
import com.nexora.sport.exception.ResourceNotFoundException;
import com.nexora.sport.model.*;
import com.nexora.sport.repository.*;
import com.nexora.sport.security.TenantScope;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class CompraService {

    private final CompraRepository compraRepository;
    private final ProveedorRepository proveedorRepository;
    private final ArticuloInventarioRepository articuloRepository;
    private final MovimientoInventarioRepository movimientoInventarioRepository;
    private final CategoriaMovimientoRepository categoriaMovimientoRepository;
    private final MovimientoFinancieroRepository movimientoFinancieroRepository;
    private final CentroRepository centroRepository;
    private final TenantScope tenantScope;

    public CompraService(CompraRepository compraRepository, ProveedorRepository proveedorRepository,
                          ArticuloInventarioRepository articuloRepository,
                          MovimientoInventarioRepository movimientoInventarioRepository,
                          CategoriaMovimientoRepository categoriaMovimientoRepository,
                          MovimientoFinancieroRepository movimientoFinancieroRepository,
                          CentroRepository centroRepository, TenantScope tenantScope) {
        this.compraRepository = compraRepository;
        this.proveedorRepository = proveedorRepository;
        this.articuloRepository = articuloRepository;
        this.movimientoInventarioRepository = movimientoInventarioRepository;
        this.categoriaMovimientoRepository = categoriaMovimientoRepository;
        this.movimientoFinancieroRepository = movimientoFinancieroRepository;
        this.centroRepository = centroRepository;
        this.tenantScope = tenantScope;
    }

    public PageResponse<CompraDto> listar(Usuario actor, Pageable pageable) {
        return PageResponse.of(compraRepository.findByCentroId(tenantScope.scopeId(actor), pageable), this::toDto);
    }

    public Compra buscar(Long id) {
        return compraRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Compra no encontrada"));
    }

    @Transactional
    public CompraDto crear(Usuario actor, CompraRequest request) {
        Long centroId = tenantScope.scopeId(actor);
        Proveedor proveedor = proveedorRepository.findById(request.proveedorId())
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado"));

        Compra compra = new Compra();
        compra.setCentro(centroRepository.getReferenceById(centroId));
        compra.setProveedor(proveedor);
        compra.setEstado(request.estado() != null ? EstadoCompra.valueOf(request.estado()) : EstadoCompra.PLANEADA);
        compra.setFechaPlaneada(request.fechaPlaneada());
        compra.setNotas(request.notas());

        BigDecimal total = BigDecimal.ZERO;
        for (var itemReq : request.items()) {
            CompraItem item = new CompraItem();
            item.setCompra(compra);
            item.setArticulo(itemReq.articuloId() != null ? articuloRepository.getReferenceById(itemReq.articuloId()) : null);
            item.setDescripcion(itemReq.descripcion());
            item.setCantidad(itemReq.cantidad());
            item.setCostoUnitario(itemReq.costoUnitario());
            compra.getItems().add(item);
            total = total.add(itemReq.costoUnitario().multiply(BigDecimal.valueOf(itemReq.cantidad())));
        }
        compra.setTotal(total);
        return toDto(compraRepository.save(compra));
    }

    @Transactional
    public CompraDto marcarRealizada(Long id, Usuario actor) {
        Compra compra = buscar(id);
        if (compra.getEstado() == EstadoCompra.REALIZADA) return toDto(compra);
        compra.setEstado(EstadoCompra.REALIZADA);
        compra.setFechaRealizada(LocalDate.now());

        for (CompraItem item : compra.getItems()) {
            if (item.getArticulo() == null) continue;
            ArticuloInventario articulo = articuloRepository.findById(item.getArticulo().getId()).orElse(null);
            if (articulo == null) continue;
            int anterior = articulo.getStock();
            int nuevo = anterior + item.getCantidad();
            articulo.setStock(nuevo);
            articuloRepository.save(articulo);

            MovimientoInventario mov = new MovimientoInventario();
            mov.setArticulo(articulo);
            mov.setTipo(TipoMovimientoInventario.COMPRA);
            mov.setStockAnterior(anterior);
            mov.setStockNuevo(nuevo);
            mov.setRazon("Compra #" + compra.getId() + " a " + compra.getProveedor().getNombre());
            mov.setRegistradoPor(actor);
            movimientoInventarioRepository.save(mov);
        }

        Long centroId = compra.getCentro().getId();
        categoriaMovimientoRepository.findByCentroIdAndNombreAndTipo(centroId, "Material", TipoMovimiento.EGRESO)
                .ifPresent(categoria -> {
                    MovimientoFinanciero mf = new MovimientoFinanciero();
                    mf.setCentro(compra.getCentro());
                    mf.setCategoria(categoria);
                    mf.setTipo(TipoMovimiento.EGRESO);
                    mf.setMonto(compra.getTotal());
                    mf.setMetodoPago(MetodoPago.TRANSFERENCIA);
                    mf.setDescripcion("Compra #" + compra.getId() + " a " + compra.getProveedor().getNombre());
                    mf.setProveedor(compra.getProveedor());
                    mf.setCompra(compra);
                    mf.setRegistradoPor(actor);
                    movimientoFinancieroRepository.save(mf);
                });

        return toDto(compraRepository.save(compra));
    }

    public CompraDto toDto(Compra c) {
        return new CompraDto(
                c.getId(), c.getProveedor().getId(), c.getProveedor().getNombre(), c.getEstado().name(),
                c.getFechaPlaneada(), c.getFechaRealizada(), c.getTotal(), c.getNotas(),
                c.getItems().stream().map(i -> new CompraDto.CompraItemDto(
                        i.getId(), i.getArticulo() != null ? i.getArticulo().getId() : null,
                        i.getDescripcion(), i.getCantidad(), i.getCostoUnitario()
                )).toList()
        );
    }
}
