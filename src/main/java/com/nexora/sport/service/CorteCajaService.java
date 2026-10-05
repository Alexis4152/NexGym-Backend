package com.nexora.sport.service;

import com.nexora.sport.dto.*;
import com.nexora.sport.exception.ResourceNotFoundException;
import com.nexora.sport.model.*;
import com.nexora.sport.repository.CentroRepository;
import com.nexora.sport.repository.CorteCajaRepository;
import com.nexora.sport.repository.UsuarioRepository;
import com.nexora.sport.repository.VentaRepository;
import com.nexora.sport.security.TenantScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Turnos de caja (abrir/cerrar) para el punto de venta de la Tienda. */
@Service
public class CorteCajaService {

    private static final Logger log = LoggerFactory.getLogger(CorteCajaService.class);

    /** Guarda, por centro, el ultimo dia en que ya corrio el cierre automatico -- evita
     * re-disparar en cada poll del minuto siguiente (igual de proposito que lastRunDate en
     * CashCut de DemoPV, aqui por centro porque cada uno configura su propia hora). En
     * memoria nada mas: si el backend se reinicia justo despues de la hora configurada,
     * en el peor caso vuelve a correr una vez mas ese dia -- inofensivo, autoCerrar() es
     * idempotente sobre un corte que ya quedo CERRADO (ejecutarCierreAutomatico solo toma
     * los que siguen ABIERTO). */
    private final Map<Long, LocalDate> ultimaEjecucionAutoCierre = new ConcurrentHashMap<>();

    private final CorteCajaRepository corteRepository;
    private final VentaRepository ventaRepository;
    private final CentroRepository centroRepository;
    private final UsuarioRepository usuarioRepository;
    private final TenantScope tenantScope;

    public CorteCajaService(CorteCajaRepository corteRepository, VentaRepository ventaRepository,
                             CentroRepository centroRepository, UsuarioRepository usuarioRepository, TenantScope tenantScope) {
        this.corteRepository = corteRepository;
        this.ventaRepository = ventaRepository;
        this.centroRepository = centroRepository;
        this.usuarioRepository = usuarioRepository;
        this.tenantScope = tenantScope;
    }

    @Transactional(readOnly = true)
    public CorteCajaDto abierto(Usuario actor) {
        return corteRepository.findFirstByUsuarioIdAndEstado(actor.getId(), EstadoCorteCaja.ABIERTO)
                .map(this::toDto).orElse(null);
    }

    /** Visibilidad en 3 niveles (seccion 32 del encargo):
     *  - Dueno/SUPER_ADMIN: todo el centro, con filtros libres (cajero, sucursal, fechas, estado).
     *  - Encargado/Administrador (ADMIN, no Supervisor): solo los cortes de SU(S) sucursal(es)
     *    autorizada(s) -- de cualquier cajero que haya trabajado ahi -- sin poder elegir otra.
     *  - Operativo (Recepcion, Caja/Ventas, etc.): solo SUS PROPIOS cortes, nunca los de un
     *    companero -- se ignora cualquier usuarioId/sucursalId que mande el cliente.
     * El filtro opcional sucursalId SOLO tiene efecto para Dueno/SUPER_ADMIN (ver nota del
     * repositorio): para los otros dos niveles la visibilidad ya viene fija, no es elegible. */
    @Transactional(readOnly = true)
    public PageResponse<CorteCajaDto> listar(Usuario actor, LocalDate desde, LocalDate hasta, String estado,
                                              Long usuarioId, Long sucursalIdFiltro, Pageable pageable) {
        Long centroId = tenantScope.scopeId(actor);
        LocalDateTime desdeFecha = desde != null ? desde.atStartOfDay() : null;
        LocalDateTime hastaFecha = hasta != null ? hasta.plusDays(1).atStartOfDay() : null;
        EstadoCorteCaja estadoEnum = (estado != null && !estado.isBlank()) ? EstadoCorteCaja.valueOf(estado) : null;

        java.util.Set<Long> sucursalesAutorizadas = null;
        Long sucursalId = null;
        Long usuarioIdEfectivo = usuarioId;
        if (tenantScope.isSupervisorOSuperior(actor)) {
            sucursalId = sucursalIdFiltro; // unico nivel que puede elegir sucursal/cajero libremente
        } else if (tenantScope.isAdminOSuperior(actor)) {
            sucursalesAutorizadas = tenantScope.sucursalesPermitidas(actor);
        } else {
            usuarioIdEfectivo = actor.getId();
        }

        return PageResponse.of(
                corteRepository.buscar(centroId, desdeFecha, hastaFecha, estadoEnum, usuarioIdEfectivo, sucursalesAutorizadas, sucursalId, pageable),
                this::toDto);
    }

    /** Cajeros para el filtro (homologado con DemoPV): no requiere seccion USUARIOS. */
    @Transactional(readOnly = true)
    public List<CajeroDto> cajeros(Usuario actor) {
        return corteRepository.cajerosDelCentro(tenantScope.scopeId(actor)).stream()
                .map(u -> new CajeroDto(u.getId(), u.getNombre())).toList();
    }

    public CorteCaja buscar(Long id) {
        return corteRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Corte de caja no encontrado"));
    }

    /** Nunca devuelve un corte de otro centro, ni uno fuera del alcance del actor (mismas
     * 3 reglas que listar(): Dueno/SUPER_ADMIN cualquiera, Encargado solo de su(s) sucursal(es),
     * Operativo solo el propio) -- evita que listar() sea la UNICA barrera y alguien vea el
     * detalle de un corte ajeno adivinando/incrementando el id. */
    public CorteCaja buscarEnAlcance(Usuario actor, Long id) {
        CorteCaja corte = buscar(id);
        if (!corte.getCentro().getId().equals(tenantScope.scopeId(actor))) {
            throw new ResourceNotFoundException("Corte de caja no encontrado");
        }
        if (tenantScope.isSupervisorOSuperior(actor)) {
            return corte;
        }
        if (tenantScope.isAdminOSuperior(actor)) {
            Long sucursalCorte = corte.getUsuario().getSucursal() != null ? corte.getUsuario().getSucursal().getId() : null;
            if (!tenantScope.sucursalPermite(actor, sucursalCorte)) {
                throw new ResourceNotFoundException("Corte de caja no encontrado");
            }
            return corte;
        }
        if (!corte.getUsuario().getId().equals(actor.getId())) {
            throw new ResourceNotFoundException("Corte de caja no encontrado");
        }
        return corte;
    }

    @Transactional(readOnly = true)
    public CorteCajaDto obtener(Usuario actor, Long id) {
        return toDto(buscarEnAlcance(actor, id));
    }

    @Transactional
    public CorteCajaDto abrir(Usuario actor, AbrirCorteRequest request) {
        Long centroId = tenantScope.scopeId(actor);
        if (corteRepository.findFirstByUsuarioIdAndEstado(actor.getId(), EstadoCorteCaja.ABIERTO).isPresent()) {
            throw new IllegalStateException("Ya tienes un corte de caja abierto");
        }
        if (!tenantScope.isAdminOSuperior(actor)) {
            LocalDateTime inicioDia = LocalDate.now().atStartOfDay();
            LocalDateTime finDia = inicioDia.plusDays(1);
            if (corteRepository.existsByUsuarioIdAndAbiertoEnBetween(actor.getId(), inicioDia, finDia)) {
                throw new IllegalStateException("Ya abriste un corte de caja hoy. Solo Dueno/Administrador puede abrir varios el mismo dia.");
            }
        }
        CorteCaja corte = new CorteCaja();
        corte.setCentro(centroRepository.getReferenceById(centroId));
        // getReferenceById (no el "actor" detached de @AuthenticationPrincipal): toDto() lee
        // c.getUsuario().getSucursal() (lazy) para armar sucursalNombre -- sobre el actor
        // detached eso revienta con LazyInitializationException (su sesion de carga ya cerro,
        // ver TenantScope#fresh); esta referencia si queda ligada a la transaccion activa.
        corte.setUsuario(usuarioRepository.getReferenceById(actor.getId()));
        corte.setMontoInicial(request.montoInicial());
        corte.setNotas(request.notas());
        return toDto(corteRepository.save(corte));
    }

    @Transactional
    public CorteCajaDto cerrar(Long id, Usuario actor, CerrarCorteRequest request) {
        CorteCaja corte = buscarEnAlcance(actor, id);
        if (corte.getEstado() != EstadoCorteCaja.ABIERTO) {
            throw new IllegalStateException("Este corte ya esta cerrado");
        }
        // Un Encargado/Administrador puede VER los cortes de su sucursal, pero solo quien lo
        // abrio (o el Dueno/SUPER_ADMIN, como excepcion administrativa) puede cerrarlo.
        if (!corte.getUsuario().getId().equals(actor.getId()) && !tenantScope.isSupervisorOSuperior(actor)) {
            throw new IllegalStateException("Solo quien abrio este corte (o el Dueno) puede cerrarlo");
        }
        BigDecimal gastos = request.gastos() != null ? request.gastos() : BigDecimal.ZERO;
        aplicarCierre(corte, gastos, request.notas(), actor);
        return toDto(corteRepository.save(corte));
    }

    @Transactional(readOnly = true)
    public CorteCajaResumenDto resumen(Usuario actor, Long id) {
        CorteCaja corte = buscarEnAlcance(actor, id);
        Totales t = sumarVentas(corte.getId());
        BigDecimal efectivoEsperado = corte.getMontoInicial().add(t.efectivo).subtract(corte.getGastos());
        return new CorteCajaResumenDto(corte.getMontoInicial(), t.efectivo, t.tarjeta, t.transferencia, t.otro,
                t.total, t.transacciones, t.canceladas, t.totalCancelado, efectivoEsperado);
    }

    /** Cierre automatico por el job programado (seccion 34 del encargo, igual que CashCut en
     * DemoPV): mismo calculo de totales que un cierre manual, sin gastos y con una nota fija
     * que lo identifica. cerradoPor queda en null a proposito -- es el marcador de "lo cerro
     * el sistema" que toDto()/el frontend usan para mostrar "Sistema" en vez de un nombre
     * (ver CortesCaja.jsx). No pasa por buscarEnAlcance ni por el candado de "solo quien lo
     * abrio puede cerrarlo": el job no tiene un actor, corre fuera de cualquier peticion. */
    public static final String NOTA_CIERRE_AUTOMATICO = "Cerrado automaticamente por el sistema (quedo abierto fuera de horario).";

    @Transactional
    public void autoCerrar(CorteCaja corte) {
        aplicarCierre(corte, BigDecimal.ZERO, NOTA_CIERRE_AUTOMATICO, null);
        corteRepository.save(corte);
    }

    /** Lo llama CorteCajaAutoCloseJob cada minuto: por cada centro con hora de cierre
     * automatico configurada (Centro#horaCierreAutomaticoCorte), si ya se paso esa hora y
     * todavia no corrio hoy para ese centro, cierra TODOS los cortes que sigan ABIERTO ahi
     * -- de cualquier cajero, de cualquier sucursal del centro. Un corte que falle no debe
     * frenar a los demas (mismo patron defensivo que NotificacionSchedulerService). */
    @Transactional
    public void ejecutarCierreAutomatico() {
        LocalDate hoy = LocalDate.now();
        LocalTime ahora = LocalTime.now();
        for (Centro centro : centroRepository.findByActivoTrueAndHoraCierreAutomaticoCorteIsNotNull()) {
            if (hoy.equals(ultimaEjecucionAutoCierre.get(centro.getId()))) continue;
            if (ahora.isBefore(centro.getHoraCierreAutomaticoCorte())) continue;
            ultimaEjecucionAutoCierre.put(centro.getId(), hoy);

            List<CorteCaja> abiertos = corteRepository.findByCentroIdAndEstado(centro.getId(), EstadoCorteCaja.ABIERTO);
            for (CorteCaja corte : abiertos) {
                try {
                    autoCerrar(corte);
                } catch (Exception e) {
                    log.error("Fallo el cierre automatico del corte {} (centro {}): {}", corte.getId(), centro.getId(), e.getMessage(), e);
                }
            }
        }
    }

    private void aplicarCierre(CorteCaja corte, BigDecimal gastos, String notas, Usuario cerradoPor) {
        Totales t = sumarVentas(corte.getId());
        corte.setGastos(gastos);
        corte.setMontoFinal(corte.getMontoInicial().add(t.efectivo).subtract(gastos));
        corte.setTotalVentas(t.total);
        corte.setVentasEfectivo(t.efectivo);
        corte.setVentasTarjeta(t.tarjeta);
        corte.setVentasTransferencia(t.transferencia);
        corte.setVentasOtro(t.otro);
        corte.setTotalTransacciones(t.transacciones);
        corte.setCanceladas(t.canceladas);
        corte.setTotalCancelado(t.totalCancelado);
        corte.setEstado(EstadoCorteCaja.CERRADO);
        corte.setCerradoEn(LocalDateTime.now());
        corte.setCerradoPor(cerradoPor);
        if (notas != null && !notas.isBlank()) corte.setNotas(notas);
    }

    private Totales sumarVentas(Long corteCajaId) {
        List<Venta> ventas = ventaRepository.findByCorteCajaId(corteCajaId);
        Totales t = new Totales();
        for (Venta v : ventas) {
            if (v.getEstado() == EstadoVenta.CANCELADA) {
                t.canceladas++;
                t.totalCancelado = t.totalCancelado.add(v.getTotal());
                continue;
            }
            t.transacciones++;
            t.total = t.total.add(v.getTotal());
            switch (v.getMetodoPago()) {
                case EFECTIVO -> t.efectivo = t.efectivo.add(v.getTotal());
                case TARJETA -> t.tarjeta = t.tarjeta.add(v.getTotal());
                case TRANSFERENCIA -> t.transferencia = t.transferencia.add(v.getTotal());
                default -> t.otro = t.otro.add(v.getTotal());
            }
        }
        return t;
    }

    private static class Totales {
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal efectivo = BigDecimal.ZERO;
        BigDecimal tarjeta = BigDecimal.ZERO;
        BigDecimal transferencia = BigDecimal.ZERO;
        BigDecimal otro = BigDecimal.ZERO;
        int transacciones = 0;
        int canceladas = 0;
        BigDecimal totalCancelado = BigDecimal.ZERO;
    }

    public CorteCajaDto toDto(CorteCaja c) {
        return new CorteCajaDto(
                c.getId(), c.getUsuario().getId(), c.getUsuario().getNombre(),
                c.getUsuario().getSucursal() != null ? c.getUsuario().getSucursal().getNombre() : null,
                c.getCerradoPor() != null ? c.getCerradoPor().getNombre() : null,
                c.getMontoInicial(), c.getMontoFinal(), c.getGastos(), c.getTotalVentas(),
                c.getVentasEfectivo(), c.getVentasTarjeta(), c.getVentasTransferencia(), c.getVentasOtro(),
                c.getTotalTransacciones(), c.getCanceladas(), c.getTotalCancelado(),
                c.getEstado().name(), c.getNotas(), c.getAbiertoEn(), c.getCerradoEn()
        );
    }
}
