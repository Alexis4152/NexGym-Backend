package com.nexora.sport.repository;

import com.nexora.sport.model.Venta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface VentaRepository extends JpaRepository<Venta, Long> {
    List<Venta> findByCorteCajaId(Long corteCajaId);
    Page<Venta> findByCentroIdOrderByCreatedAtDesc(Long centroId, Pageable pageable);

    /** Solo ventas COMPLETADA: una cancelada no debe sumar en ningun reporte. */
    @Query("select coalesce(sum(v.total), 0) from Venta v where v.centro.id = :centroId " +
           "and v.estado = com.nexora.sport.model.EstadoVenta.COMPLETADA and v.createdAt >= :desde and v.createdAt < :hasta")
    BigDecimal sumTotalCompletadas(@Param("centroId") Long centroId, @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    @Query("select count(v) from Venta v where v.centro.id = :centroId " +
           "and v.estado = com.nexora.sport.model.EstadoVenta.COMPLETADA and v.createdAt >= :desde and v.createdAt < :hasta")
    long countCompletadas(@Param("centroId") Long centroId, @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    // CAST(...) en vez de "::date": Spring Data JPA confunde "::" con un named parameter ":xxx" en native queries.
    @Query(nativeQuery = true, value =
            "select CAST(date_trunc('day', v.created_at) as date) as fecha, coalesce(sum(v.total),0) as total, count(*) as cantidad " +
            "from ventas v where v.centro_id = :centroId and v.estado = 'COMPLETADA' " +
            "and v.created_at >= :desde and v.created_at < :hasta group by 1 order by 1")
    List<Object[]> ventasPorDia(@Param("centroId") Long centroId, @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    @Query(nativeQuery = true, value =
            "select CAST(date_trunc('month', v.created_at) as date) as mes, coalesce(sum(v.total),0) as total, count(*) as cantidad " +
            "from ventas v where v.centro_id = :centroId and v.estado = 'COMPLETADA' " +
            "and v.created_at >= :desde and v.created_at < :hasta group by 1 order by 1")
    List<Object[]> ventasPorMes(@Param("centroId") Long centroId, @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    @Query(nativeQuery = true, value =
            "select v.metodo_pago as etiqueta, count(*) as cantidad, coalesce(sum(v.total),0) as total " +
            "from ventas v where v.centro_id = :centroId and v.estado = 'COMPLETADA' " +
            "and v.created_at >= :desde and v.created_at < :hasta group by 1 order by 3 desc")
    List<Object[]> ventasPorMetodoPago(@Param("centroId") Long centroId, @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    @Query(nativeQuery = true, value =
            "select u.id, u.nombre, count(*) as num_ventas, coalesce(sum(v.total),0) as total " +
            "from ventas v join usuarios u on u.id = v.usuario_id " +
            "where v.centro_id = :centroId and v.estado = 'COMPLETADA' and v.created_at >= :desde and v.created_at < :hasta " +
            "group by u.id, u.nombre order by total desc")
    List<Object[]> operacionesPorUsuario(@Param("centroId") Long centroId, @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);
}
