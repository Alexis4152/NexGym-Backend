package com.nexora.sport.service;

import com.nexora.sport.dto.reportes.*;
import com.nexora.sport.model.Membresia;
import com.nexora.sport.model.TipoMovimiento;
import com.nexora.sport.model.Usuario;
import com.nexora.sport.repository.MembresiaRepository;
import com.nexora.sport.repository.MovimientoFinancieroRepository;
import com.nexora.sport.repository.PagoMembresiaRepository;
import com.nexora.sport.repository.VentaRepository;
import com.nexora.sport.security.TenantScope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.nexora.sport.service.ReporteUtils.*;

/**
 * Reportes financieros del gimnasio (no solo Tienda). Fuente de verdad UNICA para
 * ingresos/egresos/por-concepto: {@code MovimientoFinanciero} (anulado=false) — ya
 * centraliza mensualidades, ventas, clases particulares y egresos en una sola tabla, asi
 * que sumar por categoria aqui NUNCA duplica un PagoMembresia con su movimiento hermano
 * (son la misma fila). Cartera es la unica excepcion: usa Membresia+PagoMembresia
 * directamente porque necesita el saldo POR MEMBRESIA, algo que MovimientoFinanciero no
 * modela (el es un libro de movimientos, no un estado de cuenta).
 */
@Service
public class ReporteFinancieroService {

    private final MovimientoFinancieroRepository movimientoRepository;
    private final MembresiaRepository membresiaRepository;
    private final PagoMembresiaRepository pagoMembresiaRepository;
    private final VentaRepository ventaRepository;
    private final TenantScope tenantScope;

    public ReporteFinancieroService(MovimientoFinancieroRepository movimientoRepository, MembresiaRepository membresiaRepository,
                                     PagoMembresiaRepository pagoMembresiaRepository, VentaRepository ventaRepository,
                                     TenantScope tenantScope) {
        this.movimientoRepository = movimientoRepository;
        this.membresiaRepository = membresiaRepository;
        this.pagoMembresiaRepository = pagoMembresiaRepository;
        this.ventaRepository = ventaRepository;
        this.tenantScope = tenantScope;
    }

    @Transactional(readOnly = true)
    public FinancieroResumenDto resumen(Usuario actor, LocalDate from, LocalDate to) {
        Long centroId = tenantScope.scopeId(actor);
        BigDecimal ingresos = movimientoRepository.sumMontoByTipoAndRango(centroId, TipoMovimiento.INGRESO, from, to);
        BigDecimal egresos = movimientoRepository.sumMontoByTipoAndRango(centroId, TipoMovimiento.EGRESO, from, to);
        List<EtiquetaValorDto> porConceptoIngreso = movimientoRepository.sumPorCategoria(centroId, TipoMovimiento.INGRESO, from, to)
                .stream().map(r -> new EtiquetaValorDto((Long) r[0], (String) r[1], toLong(r[2]), toBigDecimal(r[3]))).toList();
        List<EtiquetaValorDto> porConceptoEgreso = movimientoRepository.sumPorCategoria(centroId, TipoMovimiento.EGRESO, from, to)
                .stream().map(r -> new EtiquetaValorDto((Long) r[0], (String) r[1], toLong(r[2]), toBigDecimal(r[3]))).toList();
        return new FinancieroResumenDto(ingresos, egresos, ingresos.subtract(egresos), porConceptoIngreso, porConceptoEgreso);
    }

    @Transactional(readOnly = true)
    public ComparativoDto resumenComparativo(Usuario actor, LocalDate from, LocalDate to) {
        BigDecimal actual = resumen(actor, from, to).ingresos();
        long dias = to.toEpochDay() - from.toEpochDay();
        BigDecimal anterior = resumen(actor, from.minusDays(dias + 1), from.minusDays(1)).ingresos();
        return ComparativoDto.de(actual, anterior, from, to);
    }

    /**
     * Metodos de pago combinados (seccion 4.7): Tienda usa Venta.metodoPago (fuente propia,
     * no pasa por MovimientoFinanciero para desglosar POR VENTA); Membresias usa
     * PagoMembresia.metodoPago directamente (mismo motivo). El total general SI puede
     * cotejarse contra MovimientoFinanciero: ingresos totales del periodo == ventas + pagos
     * de membresia + cualquier otro ingreso manual (ver validacion en el reporte de pruebas).
     */
    @Transactional(readOnly = true)
    public Map<String, List<EtiquetaValorDto>> metodosDePago(Usuario actor, LocalDate from, LocalDate to) {
        Long centroId = tenantScope.scopeId(actor);
        List<EtiquetaValorDto> tienda = ventaRepository.ventasPorMetodoPago(centroId, from.atStartOfDay(), to.plusDays(1).atStartOfDay())
                .stream().map(r -> new EtiquetaValorDto(null, (String) r[0], toLong(r[1]), toBigDecimal(r[2]))).toList();
        List<EtiquetaValorDto> membresias = pagoMembresiaRepository.sumPorMetodoPago(centroId, from, to).stream()
                .map(r -> new EtiquetaValorDto(null, (String) r[0], toLong(r[1]), toBigDecimal(r[2]))).toList();
        Map<String, List<EtiquetaValorDto>> resultado = new HashMap<>();
        resultado.put("tienda", tienda);
        resultado.put("membresias", membresias);
        return resultado;
    }

    @Transactional(readOnly = true)
    public CarteraResumenDto cartera(Usuario actor) {
        Long centroId = tenantScope.scopeId(actor);
        List<Membresia> membresias = membresiaRepository.findParaCartera(centroId);
        if (membresias.isEmpty()) {
            return new CarteraResumenDto(BigDecimal.ZERO, 0, 0, List.of());
        }
        List<Long> ids = membresias.stream().map(Membresia::getId).toList();
        Map<Long, BigDecimal> pagadoPorMembresia = new HashMap<>();
        for (Object[] row : pagoMembresiaRepository.sumValidoAgrupadoPorMembresia(ids)) {
            pagadoPorMembresia.put((Long) row[0], toBigDecimal(row[1]));
        }

        List<CarteraItemDto> detalle = new java.util.ArrayList<>();
        BigDecimal totalPorCobrar = BigDecimal.ZERO;
        java.util.Set<Long> alumnosConSaldo = new java.util.HashSet<>();
        for (Membresia m : membresias) {
            BigDecimal pagado = pagadoPorMembresia.getOrDefault(m.getId(), BigDecimal.ZERO);
            BigDecimal saldo = m.getPrecioFinal().subtract(pagado);
            if (saldo.signum() <= 0) continue;
            detalle.add(new CarteraItemDto(m.getId(), m.getAlumno().getId(), m.getAlumno().getNombre(), m.getPlanNombreSnapshot(),
                    m.getPrecioFinal(), pagado, saldo, m.getFechaFin(), m.getEstado().name()));
            totalPorCobrar = totalPorCobrar.add(saldo);
            alumnosConSaldo.add(m.getAlumno().getId());
        }
        detalle.sort((a, b) -> b.saldo().compareTo(a.saldo()));
        return new CarteraResumenDto(totalPorCobrar, alumnosConSaldo.size(), detalle.size(), detalle);
    }
}
