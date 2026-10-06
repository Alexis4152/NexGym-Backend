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
import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface MovimientoFinancieroRepository extends JpaRepository<MovimientoFinanciero, Long> {

    /** Filtros de la pantalla de Caja (seccion 37 del encargo, mismo patron que Ventas/CorteCaja):
     *  - sucursalIds: el alcance AUTORIZADO del actor (Encargado/Admin) -- null = sin restriccion
     *    (Dueno/SUPER_ADMIN); nunca lo manda el cliente, lo calcula CajaService#listarMovimientos.
     *    Un movimiento sin sucursal propia (m.sucursal is null, ej. registrado por Dueno/SUPER_ADMIN
     *    "operando todas") se deja visible para cualquiera, igual que en findPendientes.
     *  - sucursalId/usuarioId: filtros OPCIONALES que solo tienen efecto para Dueno/SUPER_ADMIN. */
    @Query("select m from MovimientoFinanciero m where m.centro.id = :centroId and m.anulado = false " +
           "and (cast(:desde as date) is null or m.fecha >= :desde) " +
           "and (cast(:hasta as date) is null or m.fecha <= :hasta) " +
           "and (cast(:usuarioId as long) is null or m.registradoPor.id = :usuarioId) " +
           "and (:sucursalIds is null or m.sucursal is null or m.sucursal.id in :sucursalIds) " +
           "and (cast(:sucursalId as long) is null or m.sucursal.id = :sucursalId) " +
           "order by m.fecha desc, m.createdAt desc")
    Page<MovimientoFinanciero> buscar(@Param("centroId") Long centroId, @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta,
                                       @Param("usuarioId") Long usuarioId, @Param("sucursalIds") Collection<Long> sucursalIds,
                                       @Param("sucursalId") Long sucursalId, Pageable pageable);

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
