package com.nexora.sport.repository;

import com.nexora.sport.model.VentaItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * NO existe VentaItemRepository previo: los items solo se accedian via Venta#items
 * (cascade). Se crea aqui unicamente para las agregaciones de reportes; no reemplaza
 * ni duplica el flujo de venta existente (VentaService sigue manejando la escritura).
 */
public interface VentaItemRepository extends JpaRepository<VentaItem, Long> {

    @Query(nativeQuery = true, value =
            "select a.id, a.nombre, sum(vi.cantidad) as cantidad, coalesce(sum(vi.subtotal),0) as total " +
            "from venta_items vi join ventas v on v.id = vi.venta_id join articulos_inventario a on a.id = vi.articulo_id " +
            "where v.centro_id = :centroId and v.estado = 'COMPLETADA' and v.created_at >= :desde and v.created_at < :hasta " +
            "group by a.id, a.nombre order by cantidad desc limit :limit")
    List<Object[]> topProductos(@Param("centroId") Long centroId, @Param("desde") LocalDateTime desde,
                                 @Param("hasta") LocalDateTime hasta, @Param("limit") int limit);

    /**
     * costo_actual: VentaItem NO conserva un costo historico (solo precioUnitario), asi
     * que el margen usa el costo de HOY del articulo — igual limitacion documentada en
     * DemoPV (SaleItemRepository#topProductsByMargin). Si el articulo se borro, coalesce
     * a 0 para no perder la fila.
     */
    @Query(nativeQuery = true, value =
            "select a.id, a.nombre, coalesce(sum(vi.subtotal),0) as ingreso, " +
            "coalesce(sum(coalesce(a.costo,0) * vi.cantidad),0) as costo_estimado, " +
            "coalesce(sum(vi.subtotal),0) - coalesce(sum(coalesce(a.costo,0) * vi.cantidad),0) as margen " +
            "from venta_items vi join ventas v on v.id = vi.venta_id join articulos_inventario a on a.id = vi.articulo_id " +
            "where v.centro_id = :centroId and v.estado = 'COMPLETADA' and v.created_at >= :desde and v.created_at < :hasta " +
            "group by a.id, a.nombre order by margen desc limit :limit")
    List<Object[]> topProductosPorMargen(@Param("centroId") Long centroId, @Param("desde") LocalDateTime desde,
                                          @Param("hasta") LocalDateTime hasta, @Param("limit") int limit);

    @Query(nativeQuery = true, value =
            "select c.id, coalesce(c.nombre, 'Sin categoria') as etiqueta, sum(vi.cantidad) as cantidad, coalesce(sum(vi.subtotal),0) as total " +
            "from venta_items vi join ventas v on v.id = vi.venta_id join articulos_inventario a on a.id = vi.articulo_id " +
            "left join categorias_inventario c on c.id = a.categoria_id " +
            "where v.centro_id = :centroId and v.estado = 'COMPLETADA' and v.created_at >= :desde and v.created_at < :hasta " +
            "group by c.id, c.nombre order by total desc")
    List<Object[]> ventasPorCategoria(@Param("centroId") Long centroId, @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);
}
