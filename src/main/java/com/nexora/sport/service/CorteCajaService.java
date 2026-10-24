package com.nexora.sport.service;

import com.nexora.sport.dto.*;
import com.nexora.sport.exception.DiferenciaGrandeException;
import com.nexora.sport.exception.ResourceNotFoundException;
import com.nexora.sport.model.*;
import com.nexora.sport.repository.*;
import com.nexora.sport.security.TenantScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ciclo de vida de los cortes de caja (homologado con DemoPV#CashCutService): recepcion de la
 * caja al abrir, resumen en vivo, cierre con arqueo (manual, forzado por un administrador o
 * automatico) y entrega de lo contado al siguiente turno.
 *
 * Un corte es el turno de un cajero sobre una CajaRegistradora. Cada usuario tiene a lo mas un
 * corte abierto, y cada caja tambien. Al abrir, el cajero recibe lo que dejo el corte anterior
 * de esa caja (mas/menos lo que un administrador haya movido con la caja cerrada), lo cuenta y
 * da su de acuerdo; si no coincide queda una diferencia de entrega. Al cerrar, lo contado se
 * compara contra el esperado (CalculadoraCorte: incluye ventas Y pagos de membresia en
 * efectivo) y queda en la caja para el siguiente turno.
 */
@Service
public class CorteCajaService {

    private static final Logger log = LoggerFactory.getLogger(CorteCajaService.class);
    private static final NumberFormat MONEDA = NumberFormat.getCurrencyInstance(new Locale("es", "MX"));

    /** Por centro, el ultimo dia en que ya corrio el cierre automatico (evita re-disparar en cada
     * poll del minuto siguiente). En memoria: si el backend se reinicia justo despues de la hora,
     * en el peor caso corre otra vez ese dia -- inofensivo, solo toma cortes que siguen ABIERTO. */
    private final Map<Long, LocalDate> ultimaEjecucionAutoCierre = new ConcurrentHashMap<>();

    private final CorteCajaRepository corteRepository;
    private final CajaRegistradoraRepository cajaRepository;
    private final MovimientoEfectivoRepository movimientoRepository;
    private final CentroRepository centroRepository;
    private final UsuarioRepository usuarioRepository;
    private final CalculadoraCorte calculadora;
    private final MovimientoEfectivoService movimientoService;
    private final TenantScope tenantScope;
    private final BloqueoFilas bloqueoFilas;

    public CorteCajaService(CorteCajaRepository corteRepository, CajaRegistradoraRepository cajaRepository,
                            MovimientoEfectivoRepository movimientoRepository, CentroRepository centroRepository,
                            UsuarioRepository usuarioRepository, CalculadoraCorte calculadora,
                            MovimientoEfectivoService movimientoService, TenantScope tenantScope,
                            BloqueoFilas bloqueoFilas) {
        this.bloqueoFilas = bloqueoFilas;
        this.corteRepository = corteRepository;
        this.cajaRepository = cajaRepository;
        this.movimientoRepository = movimientoRepository;
        this.centroRepository = centroRepository;
        this.usuarioRepository = usuarioRepository;
        this.calculadora = calculadora;
        this.movimientoService = movimientoService;
        this.tenantScope = tenantScope;
    }

    @Transactional(readOnly = true)
    public CorteCajaDto abierto(Usuario actor) {
        return corteRepository.findFirstByUsuarioIdAndEstado(actor.getId(), EstadoCorteCaja.ABIERTO)
                .map(this::toDto).orElse(null);
    }

    /** Visibilidad en 3 niveles (seccion 32 del encargo):
     *  - Dueno/SUPER_ADMIN: todo el centro, con filtros libres (cajero, sucursal, fechas, estado).
     *  - Encargado/Administrador: solo los cortes de SU(S) sucursal(es) (la de la caja).
     *  - Operativo: solo SUS PROPIOS cortes. */
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
            sucursalId = sucursalIdFiltro;
        } else if (tenantScope.isAdminOSuperior(actor)) {
            sucursalesAutorizadas = tenantScope.sucursalesPermitidas(actor);
        } else {
            usuarioIdEfectivo = actor.getId();
        }
        return PageResponse.of(
                corteRepository.buscar(centroId, desdeFecha, hastaFecha, estadoEnum, usuarioIdEfectivo, sucursalesAutorizadas, sucursalId, pageable),
                this::toDto);
    }

    @Transactional(readOnly = true)
    public List<CajeroDto> cajeros(Usuario actor) {
        return corteRepository.cajerosDelCentro(tenantScope.scopeId(actor)).stream()
                .map(u -> new CajeroDto(u.getId(), u.getNombre())).toList();
    }

    public CorteCaja buscar(Long id) {
        return corteRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Corte de caja no encontrado"));
    }

    /** Mismas 3 reglas que listar(): nunca deja ver el detalle de un corte ajeno adivinando el id. */
    public CorteCaja buscarEnAlcance(Usuario actor, Long id) {
        CorteCaja corte = buscar(id);
        if (!corte.getCentro().getId().equals(tenantScope.scopeId(actor))) {
            throw new ResourceNotFoundException("Corte de caja no encontrado");
        }
        if (tenantScope.isSupervisorOSuperior(actor)) return corte;
        if (tenantScope.isAdminOSuperior(actor)) {
            if (!tenantScope.sucursalPermite(actor, sucursalDelCorte(corte))) {
                throw new ResourceNotFoundException("Corte de caja no encontrado");
            }
            return corte;
        }
        if (!corte.getUsuario().getId().equals(actor.getId())) {
            throw new ResourceNotFoundException("Corte de caja no encontrado");
        }
        return corte;
    }

    private static Long sucursalDelCorte(CorteCaja corte) {
        if (corte.getCajaRegistradora() != null && corte.getCajaRegistradora().getSucursal() != null) {
            return corte.getCajaRegistradora().getSucursal().getId();
        }
        return corte.getUsuario().getSucursal() != null ? corte.getUsuario().getSucursal().getId() : null;
    }

    @Transactional(readOnly = true)
    public CorteCajaDto obtener(Usuario actor, Long id) {
        return toDto(buscarEnAlcance(actor, id));
    }

    // ------------------------------------------------------------- apertura: recibir la caja
    /**
     * Abre un corte RECIBIENDO una caja: el fondo inicial es lo que el cajero conto, y se compara
     * contra lo que debia recibir (la entrega pendiente de la caja). Si no coincide queda
     * estadoEntrega=DIFERENCIA (nota obligatoria); el corte se abre de todas formas con lo
     * contado, porque es lo que de verdad hay en la caja.
     */
    @Transactional
    public CorteCajaDto abrir(Usuario actor, AbrirCorteRequest request) {
        Long centroId = tenantScope.scopeId(actor);
        if (corteRepository.findFirstByUsuarioIdAndEstado(actor.getId(), EstadoCorteCaja.ABIERTO).isPresent()) {
            throw new IllegalStateException("Ya tienes un corte de caja abierto. Cierralo antes de abrir otro.");
        }
        if (!tenantScope.isAdminOSuperior(actor)) {
            LocalDateTime inicioDia = LocalDate.now().atStartOfDay();
            if (corteRepository.existsByUsuarioIdAndAbiertoEnBetween(actor.getId(), inicioDia, inicioDia.plusDays(1))) {
                throw new IllegalStateException("Ya abriste un corte de caja hoy. Solo Dueno/Administrador puede abrir varios el mismo dia.");
            }
        }
        CajaRegistradora caja = java.util.Optional.of(bloqueoFilas.bloquear(CajaRegistradora.class, request.cajaRegistradoraId(), "Caja no encontrada"))
                .filter(c -> c.getCentro().getId().equals(centroId))
                .orElseThrow(() -> new ResourceNotFoundException("Caja no encontrada"));
        if (caja.getSucursal() != null && !tenantScope.sucursalPermite(actor, caja.getSucursal().getId())) {
            throw new IllegalStateException("La caja " + caja.getNombre() + " es de otra sucursal");
        }
        if (!caja.isActivo()) throw new IllegalStateException("La caja " + caja.getNombre() + " esta desactivada");
        corteRepository.findFirstByCajaRegistradoraIdAndEstado(caja.getId(), EstadoCorteCaja.ABIERTO).ifPresent(c -> {
            throw new IllegalStateException("La caja " + caja.getNombre() + " ya esta en uso por " + c.getUsuario().getNombre());
        });
        if (!Boolean.TRUE.equals(request.deAcuerdo())) {
            throw new IllegalStateException("Confirma que contaste el efectivo y estas de acuerdo");
        }

        BigDecimal esperado = caja.getMontoPendiente(); // null = la caja no tiene entrega previa
        BigDecimal contado = request.montoContado();
        String nota = blankToNull(request.nota());
        EstadoDiferencia estado = EstadoDiferencia.NINGUNA;
        BigDecimal diferencia = null;
        boolean confirmado = Boolean.TRUE.equals(request.confirmacionDiferenciaGrande());
        if (esperado != null && esperado.signum() == 0) {
            // La caja se entrega vacia: lo que cuente es un fondo nuevo, no una diferencia.
            if (contado.signum() > 0 && !confirmado) {
                throw new DiferenciaGrandeException("La caja se entrega vacia. Vuelve a escribir el fondo con el que empiezas para confirmarlo.");
            }
        } else if (esperado != null) {
            if (ReglasConteo.esIrrazonable(esperado, contado)) {
                throw new IllegalStateException("El monto contado no es razonable para lo que debias recibir ("
                        + MONEDA.format(esperado) + "). Revisa que no sobren digitos.");
            }
            if (ReglasConteo.esDiferenciaGrande(esperado, contado) && !confirmado) {
                throw new DiferenciaGrandeException("La diferencia con lo que debias recibir es muy grande. Vuelve a escribir el monto para confirmarlo.");
            }
            diferencia = contado.subtract(esperado);
            estado = diferencia.signum() == 0 ? EstadoDiferencia.OK : EstadoDiferencia.DIFERENCIA;
            if (estado == EstadoDiferencia.DIFERENCIA && nota == null) {
                throw new IllegalStateException("Lo que contaste no coincide con lo que debias recibir: escribe una nota");
            }
        }

        CorteCaja corte = new CorteCaja();
        corte.setCentro(centroRepository.getReferenceById(centroId));
        // getReferenceById (no el actor detached de @AuthenticationPrincipal): toDto lee asociaciones lazy.
        corte.setUsuario(usuarioRepository.getReferenceById(actor.getId()));
        corte.setCajaRegistradora(caja);
        corte.setMontoInicial(contado);
        corte.setCorteAnterior(caja.getUltimoCorte());
        corte.setMontoInicialEsperado(esperado);
        corte.setMetodoConteoRecibido(caja.getMetodoConteoPendiente());
        corte.setNotaRecepcion(nota);
        corte.setEstadoEntrega(estado);
        corte.setDiferenciaEntrega(diferencia);
        CorteCaja guardado = corteRepository.save(corte);

        // Los movimientos con la caja cerrada quedan como "los que vio este corte al recibir".
        for (MovimientoEfectivo m : movimientoRepository
                .findByCajaRegistradoraIdAndCorteCajaIsNullAndRecibidoPorCorteIsNullOrderByCreatedAtAsc(caja.getId())) {
            m.setRecibidoPorCorte(guardado);
            movimientoRepository.save(m);
        }
        // Mientras hay un corte abierto, la caja no tiene entrega pendiente.
        caja.setMontoPendiente(null);
        caja.setMetodoConteoPendiente(null);
        caja.setContadoPendientePor(null);
        caja.setContadoPendienteEn(null);
        cajaRepository.save(caja);
        return toDto(guardado);
    }

    // ------------------------------------------------------------- resumen
    /** Resumen en vivo. Con conteo ciego y el corte abierto, un cajero no ve su esperado. */
    @Transactional(readOnly = true)
    public CorteCajaResumenDto resumen(Usuario actor, Long id) {
        CorteCaja corte = buscarEnAlcance(actor, id);
        CorteCajaResumenDto r = calculadora.resumir(corte);
        if (corte.getEstado() == EstadoCorteCaja.ABIERTO && esCiegoPara(corte, actor)) {
            return CalculadoraCorte.ocultarParaConteoCiego(r);
        }
        return r;
    }

    private boolean esCiegoPara(CorteCaja corte, Usuario actor) {
        return corte.getCentro().isConteoCiego() && !movimientoService.esAdminDelCorte(corte, actor);
    }

    @Transactional(readOnly = true)
    public List<MovimientoEfectivoDto> movimientos(Usuario actor, Long id) {
        buscarEnAlcance(actor, id);
        return movimientoRepository.findByCorteCajaIdOrderByCreatedAtAsc(id).stream().map(movimientoService::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<MovimientoEfectivoDto> movimientosRecibidos(Usuario actor, Long id) {
        buscarEnAlcance(actor, id);
        return movimientoRepository.findByRecibidoPorCorteIdOrderByCreatedAtAsc(id).stream().map(movimientoService::toDto).toList();
    }

    // ------------------------------------------------------------- cierre
    /**
     * Cierra un corte con arqueo: montoContado es el efectivo que hay de verdad, y queda en la
     * caja para el siguiente turno. Si no cuadra, la nota es obligatoria (salvo conteo ciego,
     * donde el cajero no sabe si cuadro). Lo cierra su cajero (CAJERO) o un administrador
     * presente (ADMIN_PRESENCIAL).
     */
    @Transactional
    public CorteCajaDto cerrar(Long id, Usuario actor, CerrarCorteRequest request) {
        buscarEnAlcance(actor, id);
        CorteCaja corte = bloqueoFilas.bloquear(CorteCaja.class, id, "Corte de caja no encontrado");
        asegurarAbierto(corte);
        boolean propio = corte.getUsuario().getId().equals(actor.getId());
        if (!propio && !movimientoService.esAdminDelCorte(corte, actor)) {
            throw new IllegalStateException("Solo quien abrio este corte (o un administrador) puede cerrarlo");
        }
        CorteCajaResumenDto resumen = calculadora.resumir(corte);
        BigDecimal esperado = resumen.efectivoEsperado();
        BigDecimal contado = request.montoContado();
        String notas = blankToNull(request.notas());
        boolean ciego = esCiegoPara(corte, actor);
        if (ReglasConteo.esIrrazonable(esperado, contado)) {
            throw new IllegalStateException(ciego
                    ? "El monto contado no es razonable para esta caja. Revisa que no sobren digitos."
                    : "El monto contado no es razonable: el efectivo esperado es " + MONEDA.format(esperado) + ". Revisa que no sobren digitos.");
        }
        if (ReglasConteo.esDiferenciaGrande(esperado, contado) && !Boolean.TRUE.equals(request.confirmacionDiferenciaGrande())) {
            throw new DiferenciaGrandeException(ciego
                    ? "El monto es muy distinto a lo esperado. Vuelve a escribirlo para confirmarlo."
                    : "La diferencia con el efectivo esperado es muy grande. Vuelve a escribir el monto para confirmarlo.");
        }
        if (!ciego && contado.compareTo(esperado) != 0 && notas == null) {
            throw new IllegalStateException("El efectivo contado no cuadra con el esperado: escribe una nota");
        }
        Usuario actorRef = usuarioRepository.getReferenceById(actor.getId());
        cerrarInterno(corte, resumen, contado, propio ? MetodoConteo.CAJERO : MetodoConteo.ADMIN_PRESENCIAL,
                actorRef, null, notas, actorRef);
        return toDto(corte);
    }

    /**
     * Cierre forzado por un administrador (el cajero se fue sin cerrar), indicando como se conto:
     * en persona, reportado a distancia por el cajero (queda a su nombre), o sin contar.
     */
    @Transactional
    public CorteCajaDto forzarCierre(Long id, Usuario actor, ForzarCierreCorteRequest request) {
        buscarEnAlcance(actor, id);
        CorteCaja corte = bloqueoFilas.bloquear(CorteCaja.class, id, "Corte de caja no encontrado");
        if (!movimientoService.esAdminDelCorte(corte, actor)) {
            throw new IllegalStateException("Solo un administrador puede forzar el cierre de un corte");
        }
        asegurarAbierto(corte);
        CorteCajaResumenDto resumen = calculadora.resumir(corte);
        MetodoConteo metodo;
        try {
            metodo = MetodoConteo.valueOf(request.metodoConteo());
        } catch (Exception e) {
            throw new IllegalArgumentException("Metodo de conteo no valido");
        }
        BigDecimal contado = null;
        Usuario contadoPor = null;
        String evidencia = null;
        Usuario actorRef = usuarioRepository.getReferenceById(actor.getId());
        switch (metodo) {
            case ADMIN_PRESENCIAL -> {
                contado = requerirContado(request.montoContado());
                contadoPor = actorRef;
            }
            case REPORTADO_REMOTO -> {
                contado = requerirContado(request.montoContado());
                if (request.reportadoPorId() == null) throw new IllegalStateException("Indica quien reporto el conteo");
                contadoPor = movimientoService.buscarPersona(request.reportadoPorId(), corte.getCentro().getId(), actor);
                evidencia = blankToNull(request.evidenciaUrl());
            }
            case NO_CONTADO -> { /* se entrega el esperado */ }
            default -> throw new IllegalArgumentException("Metodo de conteo no valido para un cierre forzado");
        }
        if (contado != null && ReglasConteo.esIrrazonable(resumen.efectivoEsperado(), contado)) {
            throw new IllegalStateException("El monto contado no es razonable: el efectivo esperado es "
                    + MONEDA.format(resumen.efectivoEsperado()) + ". Revisa que no sobren digitos.");
        }
        String notas = truncar("Cierre forzado por " + actor.getNombre() + ": " + request.motivo().trim(), 300);
        cerrarInterno(corte, resumen, contado, metodo, contadoPor, evidencia, notas, actorRef);
        return toDto(corte);
    }

    public static final String NOTA_CIERRE_AUTOMATICO = "Cerrado automaticamente por el sistema (quedo abierto fuera de horario).";

    /** Cierre automatico (job programado): nadie conto, se entrega el esperado (NO_CONTADO) y
     * cerradoPor queda null a proposito -- marcador de "lo cerro el sistema". */
    @Transactional
    public void autoCerrar(CorteCaja corte) {
        CorteCaja c = bloqueoFilas.bloquear(CorteCaja.class, corte.getId(), "Corte de caja no encontrado");
        if (c.getEstado() != EstadoCorteCaja.ABIERTO) return;
        cerrarInterno(c, calculadora.resumir(c), null, MetodoConteo.NO_CONTADO, null, null, NOTA_CIERRE_AUTOMATICO, null);
    }

    /** Lo llama CorteCajaAutoCloseJob cada minuto: por cada centro con hora de cierre automatico
     * configurada, si ya paso esa hora y todavia no corrio hoy, cierra todos sus cortes abiertos. */
    @Transactional
    public void ejecutarCierreAutomatico() {
        LocalDate hoy = LocalDate.now();
        LocalTime ahora = LocalTime.now();
        for (Centro centro : centroRepository.findByActivoTrueAndHoraCierreAutomaticoCorteIsNotNull()) {
            if (hoy.equals(ultimaEjecucionAutoCierre.get(centro.getId()))) continue;
            if (ahora.isBefore(centro.getHoraCierreAutomaticoCorte())) continue;
            ultimaEjecucionAutoCierre.put(centro.getId(), hoy);
            for (CorteCaja corte : corteRepository.findByCentroIdAndEstado(centro.getId(), EstadoCorteCaja.ABIERTO)) {
                try {
                    autoCerrar(corte);
                } catch (Exception e) {
                    log.error("Fallo el cierre automatico del corte {} (centro {}): {}", corte.getId(), centro.getId(), e.getMessage(), e);
                }
            }
        }
    }

    /**
     * Logica comun de cierre: consolida totales, registra el arqueo (contado vs esperado) y deja
     * en la caja lo contado -- o el esperado si nadie conto -- como entrega para el siguiente turno.
     */
    private void cerrarInterno(CorteCaja corte, CorteCajaResumenDto r, BigDecimal contado, MetodoConteo metodo,
                               Usuario contadoPor, String evidencia, String notas, Usuario cerradoPor) {
        BigDecimal esperado = r.efectivoEsperado();
        BigDecimal entregado = contado != null ? contado : esperado.max(BigDecimal.ZERO);
        LocalDateTime ahora = LocalDateTime.now();

        corte.setTotalVentas(r.totalVentas());
        corte.setVentasEfectivo(r.ventasEfectivo());
        corte.setVentasTarjeta(r.ventasTarjeta());
        corte.setVentasTransferencia(r.ventasTransferencia());
        corte.setVentasOtro(r.ventasOtro());
        corte.setTotalTransacciones(r.totalTransacciones());
        corte.setCanceladas(r.canceladas());
        corte.setTotalCancelado(r.totalCancelado());
        corte.setMembresiasEfectivo(r.membresiasEfectivo());
        corte.setMembresiasTarjeta(r.membresiasTarjeta());
        corte.setMembresiasTransferencia(r.membresiasTransferencia());
        corte.setMembresiasOtro(r.membresiasOtro());
        corte.setPagosMembresia(r.pagosMembresia());
        corte.setGastos(r.totalGastos());
        corte.setTotalEntradas(r.totalEntradas());
        corte.setTotalRetiros(r.totalRetiros());
        corte.setTotalReembolsos(r.totalReembolsos());
        corte.setTotalPrestamosSalida(r.totalPrestamosSalida());
        corte.setMontoFinal(esperado);
        corte.setMontoContado(contado);
        corte.setDiferenciaEfectivo(contado != null ? contado.subtract(esperado) : null);
        corte.setMetodoConteo(metodo);
        corte.setContadoPor(contadoPor);
        corte.setEvidenciaConteoUrl(evidencia);
        corte.setMontoEntregado(entregado);
        corte.setEstadoArqueo(contado == null ? EstadoDiferencia.NINGUNA
                : contado.compareTo(esperado) == 0 ? EstadoDiferencia.OK : EstadoDiferencia.DIFERENCIA);
        corte.setEstado(EstadoCorteCaja.CERRADO);
        corte.setCerradoEn(ahora);
        corte.setCerradoPor(cerradoPor);
        if (notas != null) corte.setNotas(notas);
        corteRepository.save(corte);

        if (corte.getCajaRegistradora() != null) {
            CajaRegistradora caja = bloqueoFilas.bloquear(CajaRegistradora.class, corte.getCajaRegistradora().getId(), "Caja no encontrada");
            caja.setMontoPendiente(entregado);
            caja.setMetodoConteoPendiente(metodo);
            caja.setContadoPendientePor(contadoPor);
            caja.setContadoPendienteEn(ahora);
            caja.setUltimoCorte(corte);
            cajaRepository.save(caja);
        }
    }

    private static void asegurarAbierto(CorteCaja corte) {
        if (corte.getEstado() != EstadoCorteCaja.ABIERTO) throw new IllegalStateException("El corte ya esta cerrado");
    }

    private static BigDecimal requerirContado(BigDecimal contado) {
        if (contado == null) throw new IllegalStateException("Captura cuanto efectivo se conto");
        return contado;
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static String truncar(String s, int max) {
        return s.length() > max ? s.substring(0, max) : s;
    }

    public CorteCajaDto toDto(CorteCaja c) {
        CajaRegistradora caja = c.getCajaRegistradora();
        String sucursalNombre = caja != null && caja.getSucursal() != null ? caja.getSucursal().getNombre()
                : c.getUsuario().getSucursal() != null ? c.getUsuario().getSucursal().getNombre() : null;
        CorteCaja anterior = c.getCorteAnterior();
        return new CorteCajaDto(
                c.getId(), c.getUsuario().getId(), c.getUsuario().getNombre(), sucursalNombre,
                c.getCerradoPor() != null ? c.getCerradoPor().getNombre() : null,
                c.getMontoInicial(), c.getMontoFinal(), c.getGastos(), c.getTotalVentas(),
                c.getVentasEfectivo(), c.getVentasTarjeta(), c.getVentasTransferencia(), c.getVentasOtro(),
                c.getTotalTransacciones(), c.getCanceladas(), c.getTotalCancelado(),
                c.getEstado().name(), c.getNotas(), c.getAbiertoEn(), c.getCerradoEn(),
                caja != null ? caja.getId() : null, caja != null ? caja.getNombre() : null,
                anterior != null ? anterior.getId() : null,
                anterior != null ? anterior.getUsuario().getNombre() : null,
                c.getMontoInicialEsperado(),
                c.getMetodoConteoRecibido() != null ? c.getMetodoConteoRecibido().name() : null,
                c.getNotaRecepcion(),
                c.getEstadoEntrega() != null ? c.getEstadoEntrega().name() : null,
                c.getDiferenciaEntrega(),
                c.getMembresiasEfectivo(), c.getMembresiasTarjeta(), c.getMembresiasTransferencia(), c.getMembresiasOtro(),
                c.getPagosMembresia(), c.getTotalEntradas(), c.getTotalRetiros(), c.getTotalReembolsos(),
                c.getTotalPrestamosSalida(),
                c.getMontoContado(), c.getDiferenciaEfectivo(),
                c.getMetodoConteo() != null ? c.getMetodoConteo().name() : null,
                c.getContadoPor() != null ? c.getContadoPor().getNombre() : null,
                c.getEvidenciaConteoUrl(),
                c.getEstadoArqueo() != null ? c.getEstadoArqueo().name() : null,
                c.getMontoEntregado()
        );
    }
}
