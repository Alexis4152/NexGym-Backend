package com.nexora.sport.service;

import com.nexora.sport.dto.reportes.*;
import com.nexora.sport.model.Usuario;
import com.nexora.sport.repository.VentaItemRepository;
import com.nexora.sport.repository.VentaRepository;
import com.nexora.sport.security.TenantScope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static com.nexora.sport.service.ReporteUtils.*;

/**
 * Reportes de la Tienda (punto de venta). Fuente de verdad: {@code Venta}/{@code VentaItem}
 * con estado COMPLETADA unicamente (una venta CANCELADA nunca debe sumar). Patron de rango
 * de fechas identico al de DemoPV (ReportService): [from, to] inclusivo en LocalDate se
 * convierte a [desde, hasta) en LocalDateTime sumando un dia a "hasta".
 */
@Service
public class ReporteTiendaService {

    private final VentaRepository ventaRepository;
    private final VentaItemRepository ventaItemRepository;
    private final TenantScope tenantScope;

    public ReporteTiendaService(VentaRepository ventaRepository, VentaItemRepository ventaItemRepository, TenantScope tenantScope) {
        this.ventaRepository = ventaRepository;
        this.ventaItemRepository = ventaItemRepository;
        this.tenantScope = tenantScope;
    }

    @Transactional(readOnly = true)
    public VentaResumenDto resumen(Usuario actor, LocalDate from, LocalDate to) {
        Long centroId = tenantScope.scopeId(actor);
        LocalDateTime desde = from.atStartOfDay();
        LocalDateTime hasta = to.plusDays(1).atStartOfDay();
        BigDecimal total = ventaRepository.sumTotalCompletadas(centroId, desde, hasta);
        long numero = ventaRepository.countCompletadas(centroId, desde, hasta);
        BigDecimal ticketPromedio = numero > 0 ? total.divide(BigDecimal.valueOf(numero), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        long productosVendidos = ventaItemRepository.topProductos(centroId, desde, hasta, Integer.MAX_VALUE).stream()
                .mapToLong(row -> ((Number) row[2]).longValue()).sum();
        return new VentaResumenDto(total, numero, ticketPromedio, productosVendidos);
    }

    @Transactional(readOnly = true)
    public ComparativoDto resumenComparativo(Usuario actor, LocalDate from, LocalDate to) {
        BigDecimal actual = resumen(actor, from, to).totalVentas();
        long dias = to.toEpochDay() - from.toEpochDay();
        LocalDate desdeAnterior = from.minusDays(dias + 1);
        LocalDate hastaAnterior = from.minusDays(1);
        BigDecimal anterior = resumen(actor, desdeAnterior, hastaAnterior).totalVentas();
        return ComparativoDto.de(actual, anterior, from, to);
    }

    @Transactional(readOnly = true)
    public List<SeriePuntoDto> ventasPorDia(Usuario actor, LocalDate from, LocalDate to) {
        Long centroId = tenantScope.scopeId(actor);
        return ventaRepository.ventasPorDia(centroId, from.atStartOfDay(), to.plusDays(1).atStartOfDay()).stream()
                .map(r -> new SeriePuntoDto(toLocalDate(r[0]), toBigDecimal(r[1]), toLong(r[2]))).toList();
    }

    @Transactional(readOnly = true)
    public List<SeriePuntoDto> ventasPorMes(Usuario actor, LocalDate from, LocalDate to) {
        Long centroId = tenantScope.scopeId(actor);
        return ventaRepository.ventasPorMes(centroId, from.atStartOfDay(), to.plusDays(1).atStartOfDay()).stream()
                .map(r -> new SeriePuntoDto(toLocalDate(r[0]), toBigDecimal(r[1]), toLong(r[2]))).toList();
    }

    @Transactional(readOnly = true)
    public List<ProductoAnaliticaDto> topProductos(Usuario actor, LocalDate from, LocalDate to, int limit) {
        Long centroId = tenantScope.scopeId(actor);
        return ventaItemRepository.topProductos(centroId, from.atStartOfDay(), to.plusDays(1).atStartOfDay(), limit).stream()
                .map(r -> new ProductoAnaliticaDto((Long) r[0], (String) r[1], toLong(r[2]), toBigDecimal(r[3]), null, null)).toList();
    }

    @Transactional(readOnly = true)
    public List<ProductoAnaliticaDto> topProductosPorMargen(Usuario actor, LocalDate from, LocalDate to, int limit) {
        Long centroId = tenantScope.scopeId(actor);
        return ventaItemRepository.topProductosPorMargen(centroId, from.atStartOfDay(), to.plusDays(1).atStartOfDay(), limit).stream()
                .map(r -> new ProductoAnaliticaDto((Long) r[0], (String) r[1], 0, toBigDecimal(r[2]), toBigDecimal(r[3]), toBigDecimal(r[4]))).toList();
    }

    @Transactional(readOnly = true)
    public List<EtiquetaValorDto> ventasPorCategoria(Usuario actor, LocalDate from, LocalDate to) {
        Long centroId = tenantScope.scopeId(actor);
        return ventaItemRepository.ventasPorCategoria(centroId, from.atStartOfDay(), to.plusDays(1).atStartOfDay()).stream()
                .map(r -> new EtiquetaValorDto((Long) r[0], (String) r[1], toLong(r[2]), toBigDecimal(r[3]))).toList();
    }

    @Transactional(readOnly = true)
    public List<EtiquetaValorDto> ventasPorMetodoPago(Usuario actor, LocalDate from, LocalDate to) {
        Long centroId = tenantScope.scopeId(actor);
        return ventaRepository.ventasPorMetodoPago(centroId, from.atStartOfDay(), to.plusDays(1).atStartOfDay()).stream()
                .map(r -> new EtiquetaValorDto(null, (String) r[0], toLong(r[1]), toBigDecimal(r[2]))).toList();
    }

}
