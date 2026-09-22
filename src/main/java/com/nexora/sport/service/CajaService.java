package com.nexora.sport.service;

import com.nexora.sport.dto.CategoriaMovimientoDto;
import com.nexora.sport.dto.CategoriaMovimientoRequest;
import com.nexora.sport.dto.MovimientoFinancieroDto;
import com.nexora.sport.dto.MovimientoFinancieroRequest;
import com.nexora.sport.dto.PageResponse;
import com.nexora.sport.exception.FieldConflictException;
import com.nexora.sport.exception.ResourceNotFoundException;
import com.nexora.sport.model.*;
import com.nexora.sport.repository.*;
import com.nexora.sport.security.TenantScope;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class CajaService {

    private static final String[] CATEGORIAS_INGRESO = {
            "Inscripcion", "Mensualidad", "Clase particular", "Venta de producto", "Otro ingreso"
    };
    private static final String[] CATEGORIAS_EGRESO = {
            "Renta", "Luz", "Agua", "Internet", "Mantenimiento", "Material", "Limpieza",
            "Nomina", "Publicidad", "Otro egreso"
    };

    private final CategoriaMovimientoRepository categoriaRepository;
    private final MovimientoFinancieroRepository movimientoRepository;
    private final CentroRepository centroRepository;
    private final AlumnoRepository alumnoRepository;
    private final ProveedorRepository proveedorRepository;
    private final TenantScope tenantScope;

    public CajaService(CategoriaMovimientoRepository categoriaRepository, MovimientoFinancieroRepository movimientoRepository,
                        CentroRepository centroRepository, AlumnoRepository alumnoRepository,
                        ProveedorRepository proveedorRepository, TenantScope tenantScope) {
        this.categoriaRepository = categoriaRepository;
        this.movimientoRepository = movimientoRepository;
        this.centroRepository = centroRepository;
        this.alumnoRepository = alumnoRepository;
        this.proveedorRepository = proveedorRepository;
        this.tenantScope = tenantScope;
    }

    @Transactional
    public void seedCategoriasPorDefecto(Centro centro) {
        for (String nombre : CATEGORIAS_INGRESO) crearCategoriaSistema(centro, nombre, TipoMovimiento.INGRESO);
        for (String nombre : CATEGORIAS_EGRESO) crearCategoriaSistema(centro, nombre, TipoMovimiento.EGRESO);
    }

    private void crearCategoriaSistema(Centro centro, String nombre, TipoMovimiento tipo) {
        if (categoriaRepository.findByCentroIdAndNombreAndTipo(centro.getId(), nombre, tipo).isPresent()) return;
        CategoriaMovimiento c = new CategoriaMovimiento();
        c.setCentro(centro);
        c.setNombre(nombre);
        c.setTipo(tipo);
        c.setEsSistema(true);
        categoriaRepository.save(c);
    }

    @Transactional(readOnly = true)
    public List<CategoriaMovimientoDto> listarCategorias(Usuario actor, TipoMovimiento tipo) {
        Long centroId = tenantScope.scopeId(actor);
        var categorias = tipo != null
                ? categoriaRepository.findByCentroIdAndTipoAndActivoTrue(centroId, tipo)
                : categoriaRepository.findByCentroIdAndActivoTrue(centroId);
        return categorias.stream().map(this::toDto).toList();
    }

    @Transactional
    public CategoriaMovimientoDto crearCategoria(Usuario actor, CategoriaMovimientoRequest request) {
        Long centroId = tenantScope.scopeId(actor);
        TipoMovimiento tipo = TipoMovimiento.valueOf(request.tipo());
        if (categoriaRepository.findByCentroIdAndNombreAndTipo(centroId, request.nombre(), tipo).isPresent()) {
            throw new FieldConflictException("nombre", "Ya existe una categoria con ese nombre");
        }
        CategoriaMovimiento c = new CategoriaMovimiento();
        c.setCentro(centroRepository.getReferenceById(centroId));
        c.setNombre(request.nombre());
        c.setTipo(tipo);
        return toDto(categoriaRepository.save(c));
    }

    @Transactional(readOnly = true)
    public PageResponse<MovimientoFinancieroDto> listarMovimientos(Usuario actor, LocalDate desde, LocalDate hasta, Pageable pageable) {
        Long centroId = tenantScope.scopeId(actor);
        var page = (desde != null && hasta != null)
                ? movimientoRepository.findByCentroIdAndFechaBetweenAndAnuladoFalseOrderByFechaDesc(centroId, desde, hasta, pageable)
                : movimientoRepository.findByCentroIdAndAnuladoFalseOrderByFechaDesc(centroId, pageable);
        return PageResponse.of(page, this::toDto);
    }

    @Transactional
    public MovimientoFinancieroDto registrarMovimiento(Usuario actor, TipoMovimiento tipo, MovimientoFinancieroRequest request) {
        Long centroId = tenantScope.scopeId(actor);
        CategoriaMovimiento categoria = categoriaRepository.findById(request.categoriaId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoria no encontrada"));
        if (categoria.getTipo() != tipo) {
            throw new IllegalArgumentException("La categoria seleccionada no corresponde a un " + tipo.name().toLowerCase());
        }
        MovimientoFinanciero m = construir(centroId, tipo, categoria, request);
        m.setRegistradoPor(actor);
        return toDto(movimientoRepository.save(m));
    }

    @Transactional
    public MovimientoFinanciero registrarIngresoDeMembresia(Long centroId, Membresia membresia, BigDecimal monto,
                                                              MetodoPago metodoPago, Usuario registradoPor) {
        CategoriaMovimiento categoria = categoriaRepository.findByCentroIdAndNombreAndTipo(centroId, "Mensualidad", TipoMovimiento.INGRESO)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria 'Mensualidad' no configurada"));
        MovimientoFinanciero m = new MovimientoFinanciero();
        m.setCentro(centroRepository.getReferenceById(centroId));
        m.setCategoria(categoria);
        m.setTipo(TipoMovimiento.INGRESO);
        m.setMonto(monto);
        m.setMetodoPago(metodoPago != null ? metodoPago : MetodoPago.EFECTIVO);
        m.setDescripcion("Pago de membresia: " + membresia.getPlanNombreSnapshot());
        m.setAlumno(membresia.getAlumno());
        m.setMembresia(membresia);
        m.setRegistradoPor(registradoPor);
        return movimientoRepository.save(m);
    }

    /**
     * Anula (soft-void) un movimiento sin borrarlo, para revertir su efecto en sumas y
     * listados conservando trazabilidad. Usado hoy por PagoMembresiaService al cancelar
     * un pago/abono; queda disponible para cualquier otro flujo que necesite lo mismo
     * (ej. cancelacion de ventas, que hoy no revierte su movimiento).
     */
    @Transactional
    public void anularMovimiento(Long movimientoId, Usuario actor) {
        MovimientoFinanciero m = movimientoRepository.findById(movimientoId)
                .orElseThrow(() -> new ResourceNotFoundException("Movimiento no encontrado"));
        if (m.isAnulado()) return;
        m.setAnulado(true);
        m.setAnuladoEn(java.time.LocalDateTime.now());
        m.setAnuladoPor(actor);
        movimientoRepository.save(m);
    }

    @Transactional
    public MovimientoFinanciero registrarIngresoDeVenta(Long centroId, Venta venta, Usuario registradoPor) {
        CategoriaMovimiento categoria = categoriaRepository.findByCentroIdAndNombreAndTipo(centroId, "Venta de producto", TipoMovimiento.INGRESO)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria 'Venta de producto' no configurada"));
        MovimientoFinanciero m = new MovimientoFinanciero();
        m.setCentro(centroRepository.getReferenceById(centroId));
        m.setCategoria(categoria);
        m.setTipo(TipoMovimiento.INGRESO);
        m.setMonto(venta.getTotal());
        m.setMetodoPago(venta.getMetodoPago());
        m.setDescripcion("Venta de tienda #" + venta.getId()
                + (venta.getClienteNombre() != null && !venta.getClienteNombre().isBlank() ? " - " + venta.getClienteNombre() : ""));
        m.setRegistradoPor(registradoPor);
        return movimientoRepository.save(m);
    }

    private MovimientoFinanciero construir(Long centroId, TipoMovimiento tipo, CategoriaMovimiento categoria,
                                            MovimientoFinancieroRequest request) {
        MovimientoFinanciero m = new MovimientoFinanciero();
        m.setCentro(centroRepository.getReferenceById(centroId));
        m.setCategoria(categoria);
        m.setTipo(tipo);
        m.setMonto(request.monto());
        m.setMetodoPago(request.metodoPago() != null ? MetodoPago.valueOf(request.metodoPago()) : MetodoPago.EFECTIVO);
        m.setDescripcion(request.descripcion());
        m.setFecha(request.fecha() != null ? request.fecha() : LocalDate.now());
        if (request.alumnoId() != null) m.setAlumno(alumnoRepository.getReferenceById(request.alumnoId()));
        if (request.proveedorId() != null) m.setProveedor(proveedorRepository.getReferenceById(request.proveedorId()));
        return m;
    }

    public BigDecimal sumMonto(Long centroId, TipoMovimiento tipo, LocalDate desde, LocalDate hasta) {
        return movimientoRepository.sumMontoByTipoAndRango(centroId, tipo, desde, hasta);
    }

    public CategoriaMovimientoDto toDto(CategoriaMovimiento c) {
        return new CategoriaMovimientoDto(c.getId(), c.getNombre(), c.getTipo().name(), c.isEsSistema(), c.isActivo());
    }

    public MovimientoFinancieroDto toDto(MovimientoFinanciero m) {
        return new MovimientoFinancieroDto(
                m.getId(), m.getCategoria().getId(), m.getCategoria().getNombre(), m.getTipo().name(), m.getMonto(),
                m.getMetodoPago().name(), m.getDescripcion(), m.getFecha(),
                m.getAlumno() != null ? m.getAlumno().getId() : null,
                m.getAlumno() != null ? m.getAlumno().getNombre() : null,
                m.getProveedor() != null ? m.getProveedor().getId() : null,
                m.getProveedor() != null ? m.getProveedor().getNombre() : null
        );
    }
}
