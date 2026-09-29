package com.nexora.sport.repository;

import com.nexora.sport.model.EstadoAprobacion;
import com.nexora.sport.model.MovimientoFinanciero;
import com.nexora.sport.model.TipoMovimiento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public interface MovimientoFinancieroRepository extends JpaRepository<MovimientoFinanciero, Long> {

    Page<MovimientoFinanciero> findByCentroIdAndAnuladoFalseOrderByFechaDesc(Long centroId, Pageable pageable);

    Page<MovimientoFinanciero> findByCentroIdAndFechaBetweenAndAnuladoFalseOrderByFechaDesc(
            Long centroId, LocalDate desde, LocalDate hasta, Pageable pageable);

    /** Bandeja de aprobacion: Dueno ve todos los pendientes del centro (sucursalIds null);
     * un Encargado solo los de las sucursales que administra. */
    @Query("select m from MovimientoFinanciero m where m.centro.id = :centroId " +
           "and m.estadoAprobacion = com.nexora.sport.model.EstadoAprobacion.PENDIENTE and m.anulado = false " +
           "and (:sucursalIds is null or m.sucursal is null or m.sucursal.id in :sucursalIds) " +
           "order by m.createdAt asc")
    Page<MovimientoFinanciero> findPendientes(@Param("centroId") Long centroId,
                                               @Param("sucursalIds") Set<Long> sucursalIds, Pageable pageable);

    // solo se suman/agrupan movimientos APROBADOS: un egreso PENDIENTE o RECHAZADO no
    // debe afectar dashboard/reportes hasta que Dueno o Encargado lo resuelva.
    @Query("select coalesce(sum(m.monto), 0) from MovimientoFinanciero m " +
           "where m.centro.id = :centroId and m.tipo = :tipo and m.fecha between :desde and :hasta " +
           "and m.anulado = false and m.estadoAprobacion = com.nexora.sport.model.EstadoAprobacion.APROBADO")
    BigDecimal sumMontoByTipoAndRango(@Param("centroId") Long centroId,
                                       @Param("tipo") TipoMovimiento tipo,
                                       @Param("desde") LocalDate desde,
                                       @Param("hasta") LocalDate hasta);

    /** Agrupado por categoria (Mensualidad, Venta de producto, Clase particular, Renta, Nomina...). Fuente de verdad unica de ingresos/egresos. */
    @Query("select m.categoria.id, m.categoria.nombre, count(m), coalesce(sum(m.monto), 0) from MovimientoFinanciero m " +
           "where m.centro.id = :centroId and m.tipo = :tipo and m.fecha between :desde and :hasta " +
           "and m.anulado = false and m.estadoAprobacion = com.nexora.sport.model.EstadoAprobacion.APROBADO " +
           "group by m.categoria.id, m.categoria.nombre order by sum(m.monto) desc")
    List<Object[]> sumPorCategoria(@Param("centroId") Long centroId, @Param("tipo") TipoMovimiento tipo,
                                    @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);
}
