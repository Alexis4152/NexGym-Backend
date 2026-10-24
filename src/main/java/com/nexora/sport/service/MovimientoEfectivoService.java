package com.nexora.sport.service;

import com.nexora.sport.dto.MovimientoEfectivoDto;
import com.nexora.sport.dto.MovimientoEfectivoRequest;
import com.nexora.sport.exception.ResourceNotFoundException;
import com.nexora.sport.model.*;
import com.nexora.sport.repository.*;
import com.nexora.sport.security.TenantScope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;

/**
 * Movimientos de efectivo del cajon que no son ventas ni pagos de membresia (homologado con
 * DemoPV#CashMovementService): dentro de un corte abierto (gasto, retiro, fondo, prestamo entre
 * cajas) o con la caja cerrada (solo retiro o fondo, solo administradores).
 *
 * Regla central: una salida nunca puede sacar mas efectivo del que hay. Dentro de un corte, el
 * disponible es su efectivo esperado (CalculadoraCorte); con la caja cerrada, su entrega
 * pendiente. Un GASTO genera ademas su egreso en el libro de Caja (con la misma regla de
 * comprobante/aprobacion que un egreso manual).
 */
@Service
public class MovimientoEfectivoService {

    private static final NumberFormat MONEDA = NumberFormat.getCurrencyInstance(new Locale("es", "MX"));
    private static final Set<String> COMPROBANTE_TIPOS = Set.of("image/png", "image/jpeg", "image/webp", "application/pdf");
    private static final long COMPROBANTE_MAX = 8L * 1024 * 1024;

    private final CorteCajaRepository corteRepository;
    private final CajaRegistradoraRepository cajaRepository;
    private final MovimientoEfectivoRepository movimientoRepository;
    private final UsuarioRepository usuarioRepository;
    private final CalculadoraCorte calculadora;
    private final CajaService cajaService;
    private final FileStorageService fileStorageService;
    private final TenantScope tenantScope;
    private final BloqueoFilas bloqueoFilas;

    public MovimientoEfectivoService(CorteCajaRepository corteRepository, CajaRegistradoraRepository cajaRepository,
                                     MovimientoEfectivoRepository movimientoRepository, UsuarioRepository usuarioRepository,
                                     CalculadoraCorte calculadora, CajaService cajaService,
                                     FileStorageService fileStorageService, TenantScope tenantScope,
                                     BloqueoFilas bloqueoFilas) {
        this.bloqueoFilas = bloqueoFilas;
        this.corteRepository = corteRepository;
        this.cajaRepository = cajaRepository;
        this.movimientoRepository = movimientoRepository;
        this.usuarioRepository = usuarioRepository;
        this.calculadora = calculadora;
        this.cajaService = cajaService;
        this.fileStorageService = fileStorageService;
        this.tenantScope = tenantScope;
    }

    /**
     * Registra un movimiento dentro de un corte abierto. Lo puede hacer el dueño del corte o un
     * administrador de esa sucursal; los retiros, solo estos ultimos.
     */
    @Transactional
    public MovimientoEfectivo registrarEnCorte(Long corteId, MovimientoEfectivoRequest req, Usuario actor) {
        CorteCaja corte = java.util.Optional.of(bloqueoFilas.bloquear(CorteCaja.class, corteId, "Corte de caja no encontrado"))
                .orElseThrow(() -> new ResourceNotFoundException("Corte de caja no encontrado"));
        boolean propio = corte.getUsuario().getId().equals(actor.getId());
        boolean admin = esAdminDelCorte(corte, actor);
        if (!propio && !admin) throw new ResourceNotFoundException("Corte de caja no encontrado");
        if (corte.getEstado() != EstadoCorteCaja.ABIERTO) throw new IllegalStateException("El corte ya esta cerrado");

        TipoMovimientoEfectivo tipo = parseTipo(req.tipo());
        switch (tipo) {
            case APORTE, REEMBOLSO, PAGO_FALTANTE ->
                    throw new IllegalStateException("Este tipo de movimiento todavia no esta disponible");
            case RETIRO -> {
                if (!admin) throw new IllegalStateException("Solo un administrador puede registrar retiros");
            }
            default -> { }
        }
        BigDecimal monto = requerirMonto(req.monto());
        boolean ciego = corte.getCentro().isConteoCiego() && !admin;
        if (tipo.isSalida()) asegurarDisponible(calculadora.efectivoEsperado(corte), monto, ciego);

        MovimientoEfectivo m = nuevo(corte.getCentro(), corte.getCajaRegistradora(), tipo, monto, actor);
        m.setCorteCaja(corte);
        aplicarDetalles(m, req, corte.getCentro().getId(), actor);
        m = movimientoRepository.save(m);

        if (tipo == TipoMovimientoEfectivo.GASTO) {
            Sucursal sucursal = corte.getCajaRegistradora() != null ? corte.getCajaRegistradora().getSucursal() : null;
            MovimientoFinanciero egreso = cajaService.registrarEgresoDeCorte(actor, corte.getCentro().getId(),
                    req.categoriaId(), monto, "Gasto de caja (corte #" + corte.getId() + "): " + m.getConcepto(),
                    m.getComprobanteUrl(), sucursal);
            m.setMovimientoFinancieroId(egreso.getId());
            m = movimientoRepository.save(m);
        }
        return m;
    }

    /**
     * Retiro o entrada de efectivo con la caja cerrada (entre turnos), solo administradores.
     * Ajusta la entrega pendiente de la caja; si el admin cuenta lo que queda (restanteContado),
     * la entrega pasa a ser lo que conto, verificada por el.
     */
    @Transactional
    public MovimientoEfectivo registrarConCajaCerrada(Long cajaId, MovimientoEfectivoRequest req, Usuario actor) {
        CajaRegistradora caja = java.util.Optional.of(bloqueoFilas.bloquear(CajaRegistradora.class, cajaId, "Caja no encontrada"))
                .filter(c -> c.getCentro().getId().equals(tenantScope.scopeId(actor)))
                .orElseThrow(() -> new ResourceNotFoundException("Caja no encontrada"));
        if (!esAdminDeCaja(caja, actor)) {
            throw new IllegalStateException("Solo un administrador puede mover efectivo con la caja cerrada");
        }
        if (corteRepository.findFirstByCajaRegistradoraIdAndEstado(caja.getId(), EstadoCorteCaja.ABIERTO).isPresent()) {
            throw new IllegalStateException("La caja esta en uso: registra el movimiento dentro de su corte");
        }
        TipoMovimientoEfectivo tipo = parseTipo(req.tipo());
        if (tipo != TipoMovimientoEfectivo.RETIRO && tipo != TipoMovimientoEfectivo.FONDO) {
            throw new IllegalStateException("Con la caja cerrada solo se puede retirar o agregar efectivo");
        }
        BigDecimal monto = requerirMonto(req.monto());
        BigDecimal actual = caja.getMontoPendiente() != null ? caja.getMontoPendiente() : BigDecimal.ZERO;
        if (tipo.isSalida()) asegurarDisponible(actual, monto, false);
        BigDecimal restanteEsperado = tipo.isSalida() ? actual.subtract(monto) : actual.add(monto);

        MovimientoEfectivo m = nuevo(caja.getCentro(), caja, tipo, monto, actor);
        aplicarDetalles(m, req, caja.getCentro().getId(), actor);
        m.setRestanteEsperado(restanteEsperado);
        m.setRestanteContado(req.restanteContado());
        m = movimientoRepository.save(m);

        if (req.restanteContado() != null) {
            caja.setMontoPendiente(req.restanteContado());
            caja.setMetodoConteoPendiente(MetodoConteo.ADMIN_PRESENCIAL);
            caja.setContadoPendientePor(actor);
            caja.setContadoPendienteEn(LocalDateTime.now());
        } else {
            caja.setMontoPendiente(restanteEsperado);
        }
        cajaRepository.save(caja);
        return m;
    }

    /** Sube la foto/PDF de un comprobante (ticket de gasto, evidencia de conteo...) y devuelve su ruta. */
    public String subirComprobante(MultipartFile archivo, Usuario actor) {
        return fileStorageService.guardar("cortes-comprobantes", tenantScope.scopeId(actor), archivo,
                COMPROBANTE_TIPOS, COMPROBANTE_MAX);
    }

    // ---------------------------------------------------------------- reglas de acceso
    /** Administrador (nivel ADMIN o superior) con alcance sobre la sucursal de la caja del corte. */
    boolean esAdminDelCorte(CorteCaja corte, Usuario actor) {
        if (!corte.getCentro().getId().equals(tenantScope.scopeId(actor))) return false;
        if (corte.getCajaRegistradora() != null) return esAdminDeCaja(corte.getCajaRegistradora(), actor);
        Long sucursalCajero = corte.getUsuario().getSucursal() != null ? corte.getUsuario().getSucursal().getId() : null;
        return tenantScope.isAdminOSuperior(actor) && tenantScope.sucursalPermite(actor, sucursalCajero);
    }

    boolean esAdminDeCaja(CajaRegistradora caja, Usuario actor) {
        if (!tenantScope.isAdminOSuperior(actor)) return false;
        return caja.getSucursal() == null || tenantScope.sucursalPermite(actor, caja.getSucursal().getId());
    }

    // ---------------------------------------------------------------- helpers
    private MovimientoEfectivo nuevo(Centro centro, CajaRegistradora caja, TipoMovimientoEfectivo tipo, BigDecimal monto,
                                     Usuario actor) {
        MovimientoEfectivo m = new MovimientoEfectivo();
        m.setCentro(centro);
        m.setCajaRegistradora(caja);
        m.setTipo(tipo);
        m.setMonto(monto);
        m.setRegistradoPor(usuarioRepository.getReferenceById(actor.getId()));
        m.setOcurridoEn(LocalDateTime.now());
        return m;
    }

    /** Concepto, persona, destino, referencia y comprobante, validando lo obligatorio de cada tipo. */
    private void aplicarDetalles(MovimientoEfectivo m, MovimientoEfectivoRequest req, Long centroId, Usuario actor) {
        String concepto = blankToNull(req.concepto());
        switch (m.getTipo()) {
            case GASTO -> {
                if (concepto == null) throw new IllegalStateException("Escribe el concepto del gasto");
            }
            case RETIRO -> {
                if (req.destino() == null || req.destino().isBlank()) throw new IllegalStateException("Indica el destino del dinero");
                m.setDestino(DestinoRetiro.valueOf(req.destino()));
                m.setPersona(req.personaId() != null ? buscarPersona(req.personaId(), centroId, actor)
                        : usuarioRepository.getReferenceById(actor.getId()));
            }
            case PRESTAMO_SALIDA, PRESTAMO_ENTRADA -> {
                // Prestamo entre cajas: cada caja registra su lado por separado; aqui solo se anota la otra.
                if (req.cajaContraparteId() == null) throw new IllegalStateException("Elige la otra caja");
                CajaRegistradora otra = cajaRepository.findById(req.cajaContraparteId())
                        .filter(c -> c.getCentro().getId().equals(centroId))
                        .orElseThrow(() -> new ResourceNotFoundException("Caja no encontrada"));
                if (m.getCajaRegistradora() != null && otra.getId().equals(m.getCajaRegistradora().getId())) {
                    throw new IllegalStateException("Elige una caja distinta a la tuya");
                }
                m.setCajaContraparte(otra);
                if (concepto == null) {
                    concepto = (m.getTipo() == TipoMovimientoEfectivo.PRESTAMO_SALIDA ? "Prestamo a " : "Prestamo de ") + otra.getNombre();
                }
            }
            default -> { /* FONDO: concepto opcional */ }
        }
        m.setConcepto(concepto);
        m.setReferencia(blankToNull(req.referencia()));
        m.setComprobanteUrl(blankToNull(req.comprobanteUrl()));
    }

    /** Persona del mismo centro (o el propio actor: un SUPER_ADMIN no tiene centro propio). */
    Usuario buscarPersona(Long personaId, Long centroId, Usuario actor) {
        if (personaId.equals(actor.getId())) return usuarioRepository.getReferenceById(actor.getId());
        return usuarioRepository.findById(personaId)
                .filter(u -> u.getCentro() != null && u.getCentro().getId().equals(centroId))
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    private static TipoMovimientoEfectivo parseTipo(String tipo) {
        try {
            return TipoMovimientoEfectivo.valueOf(tipo);
        } catch (Exception e) {
            throw new IllegalArgumentException("Tipo de movimiento no valido");
        }
    }

    private static BigDecimal requerirMonto(BigDecimal monto) {
        if (monto == null || monto.signum() <= 0) throw new IllegalStateException("Captura un monto mayor a cero");
        return monto;
    }

    /** Con conteo ciego el mensaje no dice cuanto hay: revelaria el efectivo esperado. */
    private static void asegurarDisponible(BigDecimal disponible, BigDecimal monto, boolean ciego) {
        if (monto.compareTo(disponible) > 0) {
            throw new IllegalStateException(ciego
                    ? "No hay suficiente efectivo en caja para esta salida."
                    : "Solo hay " + MONEDA.format(disponible.max(BigDecimal.ZERO)) + " en caja. No se puede sacar mas efectivo del que hay.");
        }
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    public MovimientoEfectivoDto toDto(MovimientoEfectivo m) {
        return new MovimientoEfectivoDto(
                m.getId(), m.getTipo().name(), m.getTipo().isSalida(), m.getMonto(), m.getConcepto(),
                m.getPersona() != null ? m.getPersona().getId() : null,
                m.getPersona() != null ? m.getPersona().getNombre() : null,
                m.getDestino() != null ? m.getDestino().name() : null,
                m.getReferencia(), m.getComprobanteUrl(),
                m.getCajaContraparte() != null ? m.getCajaContraparte().getNombre() : null,
                m.getCorteCaja() != null ? m.getCorteCaja().getId() : null,
                m.getRegistradoPor() != null ? m.getRegistradoPor().getNombre() : null,
                m.getCreatedAt(), m.getOcurridoEn(), m.isRegistradoDespues(),
                m.getRestanteEsperado(), m.getRestanteContado(), m.getMovimientoFinancieroId());
    }
}
