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
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class CajaService {

    private static final String[] CATEGORIAS_INGRESO = {
            "Inscripcion", "Mensualidad", "Clase particular", "Venta de producto", "Otro ingreso"
    };
    private static final String[] CATEGORIAS_EGRESO = {
            "Renta", "Luz", "Agua", "Internet", "Mantenimiento", "Material", "Limpieza",
            "Nomina", "Publicidad", "Otro egreso"
    };

    private static final Set<String> COMPROBANTE_TIPOS_PERMITIDOS = Set.of("image/png", "image/jpeg", "image/webp", "application/pdf");
    private static final long COMPROBANTE_TAMANIO_MAXIMO = 8L * 1024 * 1024; // 8MB

    private final CategoriaMovimientoRepository categoriaRepository;
    private final MovimientoFinancieroRepository movimientoRepository;
    private final CentroRepository centroRepository;
    private final AlumnoRepository alumnoRepository;
    private final ProveedorRepository proveedorRepository;
    private final SucursalRepository sucursalRepository;
    private final FileStorageService fileStorageService;
    private final TenantScope tenantScope;

    public CajaService(CategoriaMovimientoRepository categoriaRepository, MovimientoFinancieroRepository movimientoRepository,
                        CentroRepository centroRepository, AlumnoRepository alumnoRepository,
                        ProveedorRepository proveedorRepository, SucursalRepository sucursalRepository,
                        FileStorageService fileStorageService, TenantScope tenantScope) {
        this.categoriaRepository = categoriaRepository;
        this.movimientoRepository = movimientoRepository;
        this.centroRepository = centroRepository;
        this.alumnoRepository = alumnoRepository;
        this.proveedorRepository = proveedorRepository;
        this.sucursalRepository = sucursalRepository;
        this.fileStorageService = fileStorageService;
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
        return registrarMovimiento(actor, tipo, request, null);
    }

    /**
     * Un INGRESO nunca pide comprobante y nace APROBADO (los ingresos reales hoy llegan
     * solos via registrarIngresoDeVenta/registrarIngresoDeMembresia; este endpoint manual
     * sigue existiendo para categorias sin flujo propio, ej. "Inscripcion"/"Otro ingreso").
     * Un EGRESO nace APROBADO si trae comprobante en el momento de registrarse, o
     * PENDIENTE si no -- en ese caso lo debe resolver Dueno o el Encargado de la sucursal
     * (ver aprobar/rechazar) antes de que cuente en sumas/reportes.
     */
    @Transactional
    public MovimientoFinancieroDto registrarMovimiento(Usuario actor, TipoMovimiento tipo, MovimientoFinancieroRequest request,
                                                         MultipartFile comprobante) {
        Long centroId = tenantScope.scopeId(actor);
        CategoriaMovimiento categoria = categoriaRepository.findById(request.categoriaId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoria no encontrada"));
        if (categoria.getTipo() != tipo) {
            throw new IllegalArgumentException("La categoria seleccionada no corresponde a un " + tipo.name().toLowerCase());
        }
        MovimientoFinanciero m = construir(centroId, tipo, categoria, request);
        m.setRegistradoPor(actor);
        Long sucursalActiva = tenantScope.sucursalActivaId(actor);
        if (sucursalActiva != null) m.setSucursal(sucursalRepository.getReferenceById(sucursalActiva));

        if (tipo == TipoMovimiento.EGRESO) {
            m = movimientoRepository.save(m); // necesita id antes de poder guardar el archivo
            if (comprobante != null && !comprobante.isEmpty()) {
                aplicarComprobante(m, comprobante);
                m.setEstadoAprobacion(EstadoAprobacion.APROBADO);
                m.setResueltoPor(actor);
                m.setResueltoEn(LocalDateTime.now());
            } else {
                m.setEstadoAprobacion(EstadoAprobacion.PENDIENTE);
            }
        }
        return toDto(movimientoRepository.save(m));
    }

    private void aplicarComprobante(MovimientoFinanciero m, MultipartFile comprobante) {
        String ruta = fileStorageService.guardar("egresos-comprobantes", m.getId(), comprobante,
                COMPROBANTE_TIPOS_PERMITIDOS, COMPROBANTE_TAMANIO_MAXIMO);
        m.setComprobanteUrl(ruta);
    }

    /** Bandeja de aprobacion (ver findPendientes): Dueno ve todos los pendientes del
     * centro; un Encargado solo los de las sucursales que administra. */
    @Transactional(readOnly = true)
    public PageResponse<MovimientoFinancieroDto> listarPendientes(Usuario actor, Pageable pageable) {
        assertPuedeResolver(actor);
        Long centroId = tenantScope.scopeId(actor);
        Set<Long> sucursalIds = tenantScope.isSupervisorOSuperior(actor) ? null : tenantScope.sucursalesPermitidas(actor);
        return PageResponse.of(movimientoRepository.findPendientes(centroId, sucursalIds, pageable), this::toDto);
    }

    @Transactional
    public MovimientoFinancieroDto aprobar(Usuario actor, Long movimientoId) {
        MovimientoFinanciero m = buscarPendiente(actor, movimientoId);
        m.setEstadoAprobacion(EstadoAprobacion.APROBADO);
        m.setResueltoPor(actor);
        m.setResueltoEn(LocalDateTime.now());
        return toDto(movimientoRepository.save(m));
    }

    @Transactional
    public MovimientoFinancieroDto rechazar(Usuario actor, Long movimientoId, String motivo) {
        MovimientoFinanciero m = buscarPendiente(actor, movimientoId);
        m.setEstadoAprobacion(EstadoAprobacion.RECHAZADO);
        m.setRechazadoMotivo(motivo);
        m.setResueltoPor(actor);
        m.setResueltoEn(LocalDateTime.now());
        return toDto(movimientoRepository.save(m));
    }

    private MovimientoFinanciero buscarPendiente(Usuario actor, Long movimientoId) {
        assertPuedeResolver(actor);
        MovimientoFinanciero m = movimientoRepository.findById(movimientoId)
                .orElseThrow(() -> new ResourceNotFoundException("Movimiento no encontrado"));
        if (m.getCentro() == null || !m.getCentro().getId().equals(tenantScope.scopeId(actor))) {
            throw new ResourceNotFoundException("Movimiento no encontrado");
        }
        if (m.getEstadoAprobacion() != EstadoAprobacion.PENDIENTE) {
            throw new IllegalStateException("Este egreso ya fue resuelto");
        }
        if (!tenantScope.isSupervisorOSuperior(actor)
                && (m.getSucursal() == null || !tenantScope.sucursalPermite(actor, m.getSucursal().getId()))) {
            throw new IllegalStateException("No administras la sucursal de este egreso");
        }
        return m;
    }

    /** Solo Dueno/SUPER_ADMIN o un Encargado (nunca Recepcion/Caja-Ventas, aunque tengan
     * CAJA_MOVIMIENTO para poder registrar) pueden aprobar o rechazar un egreso pendiente. */
    private void assertPuedeResolver(Usuario actor) {
        if (!tenantScope.isAdminOSuperior(actor)) {
            throw new IllegalStateException("Solo el Dueno o el Encargado de sucursal pueden aprobar o rechazar egresos");
        }
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
                m.getProveedor() != null ? m.getProveedor().getNombre() : null,
                m.getSucursal() != null ? m.getSucursal().getId() : null,
                m.getSucursal() != null ? m.getSucursal().getNombre() : null,
                m.getRegistradoPor() != null ? m.getRegistradoPor().getNombre() : null,
                m.getComprobanteUrl(),
                m.getEstadoAprobacion().name(),
                m.getResueltoPor() != null ? m.getResueltoPor().getNombre() : null,
                m.getResueltoEn(),
                m.getRechazadoMotivo()
        );
    }
}
