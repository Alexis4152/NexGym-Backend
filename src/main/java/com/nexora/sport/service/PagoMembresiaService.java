package com.nexora.sport.service;

import com.nexora.sport.dto.PagoMembresiaDto;
import com.nexora.sport.dto.PagoMembresiaRequest;
import com.nexora.sport.exception.ResourceNotFoundException;
import com.nexora.sport.model.*;
import com.nexora.sport.repository.MembresiaRepository;
import com.nexora.sport.repository.PagoMembresiaRepository;
import com.nexora.sport.security.TenantScope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Pagos y abonos de una Membresia. Cada pago VALIDO se integra con Caja en la MISMA
 * transaccion (ver CajaService#registrarIngresoDeMembresia) para que nunca quede un
 * pago guardado sin su movimiento, o viceversa (seccion 20 del encargo). Cancelar un
 * pago nunca lo borra: se marca CANCELADO y se anula (soft-void) su movimiento
 * hermano en Caja (seccion 21).
 */
@Service
public class PagoMembresiaService {

    private final PagoMembresiaRepository pagoMembresiaRepository;
    private final MembresiaRepository membresiaRepository;
    private final CajaService cajaService;
    private final TenantScope tenantScope;

    public PagoMembresiaService(PagoMembresiaRepository pagoMembresiaRepository, MembresiaRepository membresiaRepository,
                                 CajaService cajaService, TenantScope tenantScope) {
        this.pagoMembresiaRepository = pagoMembresiaRepository;
        this.membresiaRepository = membresiaRepository;
        this.cajaService = cajaService;
        this.tenantScope = tenantScope;
    }

    @Transactional(readOnly = true)
    public List<PagoMembresiaDto> listarPorMembresia(Usuario actor, Long membresiaId) {
        Membresia m = buscarDelCentro(membresiaId, tenantScope.scopeId(actor));
        return pagoMembresiaRepository.findByMembresiaIdOrderByFechaDescCreatedAtDesc(m.getId()).stream()
                .map(this::toDto).toList();
    }

    public BigDecimal calcularSaldo(Membresia m) {
        return m.getPrecioFinal().subtract(pagoMembresiaRepository.sumValidoByMembresiaId(m.getId()));
    }

    public BigDecimal calcularTotalPagado(Membresia m) {
        return pagoMembresiaRepository.sumValidoByMembresiaId(m.getId());
    }

    @Transactional
    public PagoMembresiaDto registrarPago(Usuario actor, Long membresiaId, PagoMembresiaRequest request) {
        Long centroId = tenantScope.scopeId(actor);
        Membresia m = buscarDelCentro(membresiaId, centroId);
        registrarPagoInterno(actor, centroId, m, request.monto(), request.metodoPago());
        return toDto(pagoMembresiaRepository.findByMembresiaIdOrderByFechaDescCreatedAtDesc(m.getId()).get(0));
    }

    /** Usado tambien por MembresiaService al contratar/renovar con pago inicial, para no duplicar las validaciones de abono. */
    @Transactional
    public PagoMembresia registrarPagoInterno(Usuario actor, Long centroId, Membresia m, BigDecimal monto, String metodoPagoStr) {
        if (m.getEstado() == EstadoMembresia.CANCELADA) {
            throw new IllegalStateException("No se puede registrar un pago en una membresia cancelada");
        }
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto del pago debe ser mayor a cero");
        }
        BigDecimal saldo = calcularSaldo(m);
        if (monto.compareTo(saldo) > 0) {
            throw new IllegalArgumentException("El monto ($" + monto + ") no puede exceder el saldo pendiente ($" + saldo + ")");
        }
        if (monto.compareTo(saldo) < 0) {
            MembresiaPlan plan = m.getPlan();
            if (!plan.isPermiteAbonos()) {
                throw new IllegalArgumentException("Este plan no permite abonos; el pago debe cubrir el total del saldo ($" + saldo + ")");
            }
            boolean esPrimerPago = pagoMembresiaRepository.countByMembresiaIdAndEstado(m.getId(), EstadoPago.VALIDO) == 0;
            if (esPrimerPago && plan.getMontoMinimoAbono() != null && monto.compareTo(plan.getMontoMinimoAbono()) < 0) {
                throw new IllegalArgumentException("El pago inicial minimo para este plan es $" + plan.getMontoMinimoAbono());
            }
        }

        MetodoPago metodo = metodoPagoStr != null ? MetodoPago.valueOf(metodoPagoStr) : MetodoPago.EFECTIVO;
        MovimientoFinanciero movimiento = cajaService.registrarIngresoDeMembresia(centroId, m, monto, metodo, actor);

        PagoMembresia pago = new PagoMembresia();
        pago.setMembresia(m);
        pago.setMonto(monto);
        pago.setMetodoPago(metodo);
        pago.setMovimientoFinanciero(movimiento);
        pago.setRegistradoPor(actor);
        return pagoMembresiaRepository.save(pago);
    }

    @Transactional
    public PagoMembresiaDto cancelarPago(Usuario actor, Long membresiaId, Long pagoId, String motivo) {
        if (!tenantScope.isAdminOSuperior(actor)) {
            throw new IllegalStateException("Solo Dueno o Administrador puede cancelar un pago");
        }
        Long centroId = tenantScope.scopeId(actor);
        Membresia m = buscarDelCentro(membresiaId, centroId);
        PagoMembresia pago = pagoMembresiaRepository.findById(pagoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pago no encontrado"));
        if (!pago.getMembresia().getId().equals(m.getId())) {
            throw new ResourceNotFoundException("Pago no encontrado");
        }
        if (pago.getEstado() == EstadoPago.CANCELADO) {
            throw new IllegalStateException("Este pago ya esta cancelado");
        }
        pago.setEstado(EstadoPago.CANCELADO);
        pago.setMotivoCancelacion(motivo);
        pago.setCanceladoEn(LocalDateTime.now());
        pago.setCanceladoPor(actor);
        if (pago.getMovimientoFinanciero() != null) {
            cajaService.anularMovimiento(pago.getMovimientoFinanciero().getId(), actor);
        }
        return toDto(pagoMembresiaRepository.save(pago));
    }

    private Membresia buscarDelCentro(Long membresiaId, Long centroId) {
        Membresia m = membresiaRepository.findById(membresiaId)
                .orElseThrow(() -> new ResourceNotFoundException("Membresia no encontrada"));
        if (!m.getCentro().getId().equals(centroId)) {
            throw new ResourceNotFoundException("Membresia no encontrada");
        }
        return m;
    }

    public PagoMembresiaDto toDto(PagoMembresia p) {
        return new PagoMembresiaDto(
                p.getId(), p.getMembresia().getId(), p.getMonto(), p.getMetodoPago().name(), p.getFecha(),
                p.getEstado().name(), p.getRegistradoPor() != null ? p.getRegistradoPor().getNombre() : null,
                p.getMotivoCancelacion(), p.getCanceladoEn(),
                p.getCanceladoPor() != null ? p.getCanceladoPor().getNombre() : null, p.getCreatedAt()
        );
    }
}
