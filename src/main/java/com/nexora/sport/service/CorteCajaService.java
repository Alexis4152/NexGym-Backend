package com.nexora.sport.service;

import com.nexora.sport.dto.*;
import com.nexora.sport.exception.ResourceNotFoundException;
import com.nexora.sport.model.*;
import com.nexora.sport.repository.CentroRepository;
import com.nexora.sport.repository.CorteCajaRepository;
import com.nexora.sport.repository.VentaRepository;
import com.nexora.sport.security.TenantScope;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Turnos de caja (abrir/cerrar) para el punto de venta de la Tienda. */
@Service
public class CorteCajaService {

    private final CorteCajaRepository corteRepository;
    private final VentaRepository ventaRepository;
    private final CentroRepository centroRepository;
    private final TenantScope tenantScope;

    public CorteCajaService(CorteCajaRepository corteRepository, VentaRepository ventaRepository,
                             CentroRepository centroRepository, TenantScope tenantScope) {
        this.corteRepository = corteRepository;
        this.ventaRepository = ventaRepository;
        this.centroRepository = centroRepository;
        this.tenantScope = tenantScope;
    }

    @Transactional(readOnly = true)
    public CorteCajaDto abierto(Usuario actor) {
        return corteRepository.findFirstByUsuarioIdAndEstado(actor.getId(), EstadoCorteCaja.ABIERTO)
                .map(this::toDto).orElse(null);
    }

    @Transactional(readOnly = true)
    public PageResponse<CorteCajaDto> listar(Usuario actor, Pageable pageable) {
        return PageResponse.of(corteRepository.findByCentroId(tenantScope.scopeId(actor), pageable), this::toDto);
    }

    public CorteCaja buscar(Long id) {
        return corteRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Corte de caja no encontrado"));
    }

    @Transactional(readOnly = true)
    public CorteCajaDto obtener(Long id) {
        return toDto(buscar(id));
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
        corte.setUsuario(actor);
        corte.setMontoInicial(request.montoInicial());
        corte.setNotas(request.notas());
        return toDto(corteRepository.save(corte));
    }

    @Transactional
    public CorteCajaDto cerrar(Long id, Usuario actor, CerrarCorteRequest request) {
        CorteCaja corte = buscar(id);
        if (corte.getEstado() != EstadoCorteCaja.ABIERTO) {
            throw new IllegalStateException("Este corte ya esta cerrado");
        }
        BigDecimal gastos = request.gastos() != null ? request.gastos() : BigDecimal.ZERO;
        aplicarCierre(corte, gastos, request.notas(), actor);
        return toDto(corteRepository.save(corte));
    }

    @Transactional(readOnly = true)
    public CorteCajaResumenDto resumen(Long id) {
        CorteCaja corte = buscar(id);
        Totales t = sumarVentas(corte.getId());
        BigDecimal efectivoEsperado = corte.getMontoInicial().add(t.efectivo).subtract(corte.getGastos());
        return new CorteCajaResumenDto(corte.getMontoInicial(), t.efectivo, t.tarjeta, t.transferencia, t.otro,
                t.total, t.transacciones, t.canceladas, t.totalCancelado, efectivoEsperado);
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
                c.getCerradoPor() != null ? c.getCerradoPor().getNombre() : null,
                c.getMontoInicial(), c.getMontoFinal(), c.getGastos(), c.getTotalVentas(),
                c.getVentasEfectivo(), c.getVentasTarjeta(), c.getVentasTransferencia(), c.getVentasOtro(),
                c.getTotalTransacciones(), c.getCanceladas(), c.getTotalCancelado(),
                c.getEstado().name(), c.getNotas(), c.getAbiertoEn(), c.getCerradoEn()
        );
    }
}
